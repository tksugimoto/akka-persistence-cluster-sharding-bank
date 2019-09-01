package io.github.tksugimoto.bank.account

import java.time.Duration

import akka.Done
import akka.actor.{ActorRef, ActorSystem, Status}
import akka.cluster.Cluster
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

class AccountSpec
    extends TestKit(ActorSystem())
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val generateUniqueId: () => AccountId = {
    var count = 0L
    () => {
      count += 1
      AccountId(count)
    }
  }

  implicit val timeout: Timeout = Timeout.create(
    system.settings.config
      .getDuration("io.github.tksugimoto.bank.account.processing-timeout"),
  )

  val suspendAfter: Duration = system.settings.config
    .getDuration("io.github.tksugimoto.bank.account.suspend-after")

  val cluster: Cluster = Cluster(system)
  cluster.join(cluster.selfAddress)
  val accountShardRegion: ActorRef = Account.Sharding.startClusterSharding()

  "Account" must {
    "balanceの初期値は0" in {
      val accountId = generateUniqueId()
      val account = accountShardRegion
      account ! Account.GetBalance(accountId)
      expectMsg(0)
    }

    "入金で残高が増える" in {
      val accountId = generateUniqueId()
      val account = accountShardRegion
      account ! Account.Deposit(accountId, 200)
      expectMsg(Done)
      account ! Account.Deposit(accountId, 100)
      expectMsg(Done)
      account ! Account.GetBalance(accountId)
      expectMsg(300)
    }

    "出金で残高が減る" in {
      val accountId = generateUniqueId()
      val account = accountShardRegion
      account ! Account.Deposit(accountId, 200)
      expectMsg(Done)
      account ! Account.Withdraw(accountId, 150)
      expectMsg(Done)
      account ! Account.GetBalance(accountId)
      expectMsg(50)
    }

    "残高以上に出金できない" in {
      val accountId = generateUniqueId()
      val account = accountShardRegion
      val amount = 123
      account ! Account.Withdraw(accountId, amount)
      expectMsgPF() {
        case Status.Failure(ex) =>
          ex shouldBe a[IllegalArgumentException]
          ex.getMessage should be("残高不足")
      }
    }

    "入金を並列実行しても不整合が発生しない" in {
      val accountId = generateUniqueId()
      val account = accountShardRegion
      val loopCount = 100000
      val amount = 1

      (1 to loopCount).foreach { _ =>
        account ! Account.Deposit(accountId, amount)
      }
      receiveN(loopCount)

      account ! Account.GetBalance(accountId)
      expectMsg(amount * loopCount)
    }

    "一定時間処理がない場合一時停止する" in {
      val accountId = generateUniqueId()
      accountShardRegion ! Account.Deposit(accountId, 100)
      expectMsg(Done)
      val account = lastSender

      watch(account)

      Thread.sleep(suspendAfter.toMillis)
      expectTerminated(account)
    }

    "一時停止後も残高は保持されている" in {
      val accountId = generateUniqueId()
      accountShardRegion ! Account.Deposit(accountId, 100)
      expectMsg(Done)
      val account1 = lastSender
      watch(account1)

      Thread.sleep(suspendAfter.toMillis)
      expectTerminated(account1)

      accountShardRegion ! Account.GetBalance(accountId)
      expectMsg(100)
    }
  }
}

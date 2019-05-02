package io.github.tksugimoto.bank.account

import akka.Done
import akka.actor.{ActorSystem, Status}
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

  val generateUniqueId: () => Long = {
    var count = 0L
    () => {
      count += 1
      count
    }
  }

  implicit val timeout: Timeout = Timeout.create(
    system.settings.config
      .getDuration("io.github.tksugimoto.bank.account.processing-timeout"),
  )

  "Account" must {

    val accountService = Account.startService()

    "balanceの初期値は0" in {
      val accountId = generateUniqueId()
      accountService ! Account.GetBalance(accountId)
      expectMsg(0)
    }

    "入金で残高が増える" in {
      val accountId = generateUniqueId()
      accountService ! Account.Deposit(accountId, 200)
      expectMsg(Done)
      accountService ! Account.Deposit(accountId, 100)
      expectMsg(Done)
      accountService ! Account.GetBalance(accountId)
      expectMsg(300)
    }

    "出金で残高が減る" in {
      val accountId = generateUniqueId()
      accountService ! Account.Deposit(accountId, 200)
      expectMsg(Done)
      accountService ! Account.Withdraw(accountId, 150)
      expectMsg(Done)
      accountService ! Account.GetBalance(accountId)
      expectMsg(50)
    }

    "残高以上に出金できない" in {
      val accountId = generateUniqueId()
      val amount = 123
      accountService ! Account.Withdraw(accountId, amount)
      expectMsgPF() {
        case Status.Failure(ex) =>
          ex shouldBe a[IllegalArgumentException]
          ex.getMessage should be("残高不足")
      }
    }

    "入金を並列実行しても不整合が発生しない" in {
      val accountId = generateUniqueId()
      val loopCount = 100000
      val amount = 1

      (1 to loopCount).foreach { _ =>
        accountService ! Account.Deposit(accountId, amount)
      }
      receiveN(loopCount)

      accountService ! Account.GetBalance(accountId)
      expectMsg(amount * loopCount)
    }
  }
}

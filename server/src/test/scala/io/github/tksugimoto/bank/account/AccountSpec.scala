package io.github.tksugimoto.bank.account

import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.util.Timeout
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class AccountSpec
    extends TestKit(ActorSystem())
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

    Account.start()

    "balanceの初期値は0" in {
      val accountId = generateUniqueId()
      Account.balance(accountId).futureValue shouldEqual 0
    }

    "入金で残高が増える" in {
      val accountId = generateUniqueId()
      Account.deposit(accountId, 200)
      Account.deposit(accountId, 100)
      Account.balance(accountId).futureValue shouldEqual 300
    }

    "出金で残高が減る" in {
      val accountId = generateUniqueId()
      Account.deposit(accountId, 200)
      Account.withdraw(accountId, 150)
      Account.balance(accountId).futureValue shouldEqual 50
    }

    "残高以上に出金できない" in {
      val accountId = generateUniqueId()
      val amount = 123
      val result = Account.withdraw(accountId, amount)
      result.failed.futureValue shouldBe a[IllegalArgumentException]
      result.failed.futureValue.getMessage should be("残高不足")
    }

    "入金を並列実行しても不整合が発生しない" in {
      val accountId = generateUniqueId()
      val loopCount = 100000
      val amount = 1

      import scala.concurrent.ExecutionContext.Implicits.global

      val futures = (1 to loopCount).map { _ =>
        Account.deposit(accountId, amount)
      }

      Await.ready(Future.sequence(futures), Duration.Inf)
      Account.balance(accountId).futureValue shouldEqual (amount * loopCount)
    }
  }
}

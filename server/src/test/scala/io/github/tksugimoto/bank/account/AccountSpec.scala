package io.github.tksugimoto.bank.account

import org.scalatest.EitherValues._
import org.scalatest._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class AccountSpec extends WordSpecLike with Matchers {
  val generateUniqueId: () => Long = {
    var count = 0L
    () => {
      count += 1
      count
    }
  }

  "Account" must {
    "balanceの初期値は0" in {
      val accountId = generateUniqueId()
      Account.balance(accountId) shouldEqual 0
    }

    "入金で残高が増える" in {
      val accountId = generateUniqueId()
      Account.deposit(accountId, 200)
      Account.deposit(accountId, 100)
      Account.balance(accountId) shouldEqual 300
    }

    "出金で残高が減る" in {
      val accountId = generateUniqueId()
      Account.deposit(accountId, 200)
      Account.withdraw(accountId, 150)
      Account.balance(accountId) shouldEqual 50
    }

    "残高以上に出金できない" in {
      val accountId = generateUniqueId()
      val amount = 123
      val result = Account.withdraw(accountId, amount)
      result.left.value shouldBe a[IllegalArgumentException]
      result.left.value.getMessage should be("残高不足")
    }

    "入金を並列実行しても不整合が発生しない" in {
      val accountId = generateUniqueId()
      val loopCount = 100000
      val amount = 1

      import scala.concurrent.ExecutionContext.Implicits.global

      val futures = (1 to loopCount).map { _ =>
        Future {
          Account.deposit(accountId, amount)
        }
      }

      Await.ready(Future.sequence(futures), Duration.Inf)
      Account.balance(accountId) shouldEqual (amount * loopCount)
    }
  }
}

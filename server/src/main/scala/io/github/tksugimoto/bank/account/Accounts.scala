package io.github.tksugimoto.bank.account

import akka.Done
import akka.actor.{Actor, Props, Status}

object Accounts {
  def props(): Props = Props(new Accounts())
}

class Accounts extends Actor {
  private var balances: Map[AccountId, Balance] =
    Map.empty[AccountId, Balance].withDefaultValue(0)

  override def receive: Receive = {
    case Account.Deposit(accountId, amount) =>
      val currentBalance = balances(accountId)
      balances += (accountId -> (currentBalance + amount))
      sender() ! Done

    case Account.Withdraw(accountId, amount) =>
      val currentBalance = balances(accountId)
      val updatedBalance = currentBalance - amount
      if (updatedBalance < 0) {
        sender() ! Status.Failure(new IllegalArgumentException("残高不足"))
      } else {
        balances += (accountId -> updatedBalance)
        sender() ! Done
      }

    case Account.GetBalance(accountId) =>
      sender() ! balances(accountId)
  }
}

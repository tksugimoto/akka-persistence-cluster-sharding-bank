package io.github.tksugimoto.bank.account

import scala.collection.mutable

object Account {
  private val balances: mutable.Map[AccountId, Balance] =
    mutable.Map.empty[AccountId, Balance].withDefaultValue(0)

  def deposit(accountId: AccountId, amount: Int): Unit = {
    // TODO: データの同時更新対策
    val currentBalance = balances(accountId)
    balances.update(accountId, currentBalance + amount)
  }

  def withdraw(
      accountId: AccountId,
      amount: Int,
  ): Either[IllegalArgumentException, Unit] = {
    // TODO: データの同時更新対策
    val currentBalance = balances(accountId)
    val updatedBalance = currentBalance - amount
    if (updatedBalance < 0) {
      Left(new IllegalArgumentException("残高不足"))
    } else {
      balances.update(accountId, updatedBalance)
      Right(())
    }
  }

  def balance(accountId: AccountId): Balance = {
    balances(accountId)
  }
}

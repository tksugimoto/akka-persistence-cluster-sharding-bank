package io.github.tksugimoto.bank.account

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Future

object Account {
  sealed trait Command {
    def accountId: AccountId
  }
  case class Deposit(accountId: AccountId, amount: Int) extends Command
  case class Withdraw(accountId: AccountId, amount: Int) extends Command
  case class GetBalance(accountId: AccountId) extends Command

  private var accountsActor: ActorRef = _
  def start()(implicit system: ActorSystem): Unit = {
    if (accountsActor == null) {
      accountsActor = system.actorOf(Accounts.props())
    }
  }

  def deposit(accountId: AccountId, amount: Int)(
      implicit timeout: Timeout,
  ): Future[Done] = {
    (accountsActor ? Deposit(accountId, amount)).mapTo[Done]
  }

  def withdraw(
      accountId: AccountId,
      amount: Int,
  )(implicit timeout: Timeout): Future[Done] = {
    (accountsActor ? Withdraw(accountId, amount)).mapTo[Done]
  }

  def balance(
      accountId: AccountId,
  )(implicit timeout: Timeout): Future[Balance] = {
    (accountsActor ? GetBalance(accountId)).mapTo[Balance]
  }
}

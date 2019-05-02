package io.github.tksugimoto.bank.account

import akka.actor.{ActorRef, ActorSystem}

object Account {
  sealed trait Command {
    def accountId: AccountId
  }
  case class Deposit(accountId: AccountId, amount: Int) extends Command
  case class Withdraw(accountId: AccountId, amount: Int) extends Command
  case class GetBalance(accountId: AccountId) extends Command

  def startService()(implicit system: ActorSystem): ActorRef =
    system.actorOf(Accounts.props(), name = "accounts")
}

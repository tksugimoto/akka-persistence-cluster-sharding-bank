package io.github.tksugimoto.bank.account

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Status}

object Account {
  sealed trait Command {
    def accountId: AccountId
  }
  case class Deposit(accountId: AccountId, amount: Int) extends Command
  case class Withdraw(accountId: AccountId, amount: Int) extends Command
  case class GetBalance(accountId: AccountId) extends Command

  def startService()(implicit system: ActorSystem): ActorRef =
    system.actorOf(Accounts.props(), name = "accounts")

  def props(): Props = Props(new Account())
}

class Account extends Actor with ActorLogging {
  import Account._

  log.info("created")

  var balance: Balance = 0

  override def receive: Receive = {
    case Deposit(_, amount) =>
      balance += amount
      sender() ! Done

    case Withdraw(_, amount) =>
      val currentBalance = balance
      val updatedBalance = currentBalance - amount
      if (updatedBalance < 0) {
        sender() ! Status.Failure(new IllegalArgumentException("残高不足"))
      } else {
        balance = updatedBalance
        sender() ! Done
      }

    case GetBalance(_) =>
      sender() ! balance
  }
}

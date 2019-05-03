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

  sealed trait Event
  case class Deposited(amount: Int) extends Event
  case class Withdrew(amount: Int) extends Event

  def startService()(implicit system: ActorSystem): ActorRef =
    system.actorOf(Accounts.props(), name = "accounts")

  def props(): Props = Props(new Account())
}

class Account extends Actor with ActorLogging {
  import Account._

  log.info("created")

  var balance: Balance = 0

  def updateState(event: Event): Unit = event match {
    case Deposited(amount) =>
      balance += amount

    case Withdrew(amount) =>
      balance -= amount
  }

  override def receive: Receive = {
    case Deposit(_, amount) =>
      updateState(Deposited(amount))
      sender() ! Done

    case Withdraw(_, amount) =>
      val currentBalance = balance
      val updatedBalance = currentBalance - amount
      if (updatedBalance < 0) {
        sender() ! Status.Failure(new IllegalArgumentException("残高不足"))
      } else {
        updateState(Withdrew(amount))
        sender() ! Done
      }

    case GetBalance(_) =>
      sender() ! balance
  }
}

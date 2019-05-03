package io.github.tksugimoto.bank.account

import akka.Done
import akka.actor.{ActorLogging, ActorRef, ActorSystem, Props, Status}
import akka.persistence.{PersistentActor, RecoveryCompleted}

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

class Account extends PersistentActor with ActorLogging {
  import Account._

  log.info("created")

  override def persistenceId: String = s"${context.self.path.name}"

  var balance: Balance = 0

  def updateState(event: Event): Unit = event match {
    case Deposited(amount) =>
      balance += amount

    case Withdrew(amount) =>
      balance -= amount
  }

  override def receiveCommand: Receive = {
    case Deposit(_, amount) =>
      persist(Deposited(amount)) { event =>
        updateState(event)
        sender() ! Done
      }

    case Withdraw(_, amount) =>
      val currentBalance = balance
      val updatedBalance = currentBalance - amount
      if (updatedBalance < 0) {
        sender() ! Status.Failure(new IllegalArgumentException("残高不足"))
      } else {
        persist(Withdrew(amount)) { event =>
          updateState(event)
          sender() ! Done
        }
      }

    case GetBalance(_) =>
      sender() ! balance
  }

  override def receiveRecover: Receive = {
    case event: Event      => updateState(event)
    case RecoveryCompleted => log.info("RecoveryCompleted")
  }
}

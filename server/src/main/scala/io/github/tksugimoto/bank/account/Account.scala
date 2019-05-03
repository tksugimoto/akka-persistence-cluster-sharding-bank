package io.github.tksugimoto.bank.account

import akka.Done
import akka.actor.{
  ActorLogging,
  ActorRef,
  ActorSystem,
  Props,
  ReceiveTimeout,
  Status,
}
import akka.persistence.{PersistentActor, RecoveryCompleted}

import scala.concurrent.duration.Duration

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

  context.setReceiveTimeout(
    Duration.fromNanos(
      context.system.settings.config
        .getDuration("io.github.tksugimoto.bank.account.suspend-after")
        .toNanos,
    ),
  )

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

    case ReceiveTimeout =>
      log.info("shutdown")
      context.stop(self)

  }

  override def receiveRecover: Receive = {
    case event: Event      => updateState(event)
    case RecoveryCompleted => log.info("RecoveryCompleted")
  }
}

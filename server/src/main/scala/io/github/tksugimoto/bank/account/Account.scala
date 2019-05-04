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
import akka.cluster.sharding.ShardRegion.Passivate
import akka.cluster.sharding.{
  ClusterSharding,
  ClusterShardingSettings,
  ShardRegion,
}
import akka.persistence.{PersistentActor, RecoveryCompleted}

import scala.concurrent.duration.Duration

object Account {
  sealed trait Command {
    def accountId: AccountId
  }
  case class Deposit(accountId: AccountId, amount: Amount) extends Command
  case class Withdraw(accountId: AccountId, amount: Amount) extends Command
  case class GetBalance(accountId: AccountId) extends Command

  sealed trait Event {
    def accountId: AccountId
  }
  case class Deposited(accountId: AccountId, amount: Amount) extends Event
  case class Withdrew(accountId: AccountId, amount: Amount) extends Event

  object Sharding {
    case object Stop

    def startClusterSharding()(implicit system: ActorSystem): ActorRef =
      ClusterSharding(system).start(
        typeName = "Account",
        entityProps = props(),
        settings = ClusterShardingSettings(system),
        extractEntityId = extractEntityId,
        extractShardId = extractShardId,
      )

    private val extractEntityId: ShardRegion.ExtractEntityId = {
      case command: Command => (command.accountId.value.toString, command)
    }

    private val numberOfShards = 100

    private val extractShardId: ShardRegion.ExtractShardId = {
      case command: Command =>
        (command.accountId.value % numberOfShards).toString
    }
  }

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

  var balance: Balance = Balance(0)

  def updateState(event: Event): Unit = event match {
    case event: Deposited =>
      balance += event.amount

    case event: Withdrew =>
      balance -= event.amount
  }

  override def receiveCommand: Receive = {
    case Deposit(accountId, amount) =>
      persist(Deposited(accountId, amount)) { event =>
        updateState(event)
        sender() ! Done
      }

    case Withdraw(accountId, amount) =>
      val currentBalance = balance
      val updatedBalance = currentBalance - amount
      if (updatedBalance.isNegative) {
        sender() ! Status.Failure(new IllegalArgumentException("残高不足"))
      } else {
        persist(Withdrew(accountId, amount)) { event =>
          updateState(event)
          sender() ! Done
        }
      }

    case GetBalance(_) =>
      sender() ! balance

    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = Sharding.Stop)

    case Sharding.Stop =>
      log.info("shutdown")
      context.stop(self)

  }

  override def receiveRecover: Receive = {
    case event: Event      => updateState(event)
    case RecoveryCompleted => log.info("RecoveryCompleted")
  }
}

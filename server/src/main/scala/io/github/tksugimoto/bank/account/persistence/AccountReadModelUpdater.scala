package io.github.tksugimoto.bank.account.persistence

import java.util.UUID

import akka.Done
import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{
  EventEnvelope,
  Offset,
  PersistenceQuery,
  TimeBasedUUID,
}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink}
import io.github.tksugimoto.bank.account.models.Tables
import io.github.tksugimoto.bank.account.{Account, AccountId, Amount, Balance}

import scala.concurrent.{ExecutionContext, Future}

object AccountReadModelUpdater {

  import Tables.profile.api._

  val tag = "account-event"

  private val db =
    Database.forConfig("io.github.tksugimoto.bank.account.persistence.db")

  def start()(
      implicit system: ActorSystem,
      materializer: ActorMaterializer,
  ): Unit = {

    import system.dispatcher

    val readJournal = PersistenceQuery(system)
      .readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

    db.run(fetchOffsetDbio()) map { maybeOffset =>
      val offset: Offset = maybeOffset.getOrElse(Offset.noOffset)

      val source = readJournal.eventsByTag(tag, offset)

      val flow = Flow[EventEnvelope].mapAsync(parallelism = 1) { envelope =>
        envelope.offset match {
          case TimeBasedUUID(uuid) =>
            envelope.event match {
              case event: Account.Event =>
                val accountId = event.accountId
                val diff = event match {
                  case event: Account.Deposited => +event.amount
                  case event: Account.Withdrew  => -event.amount
                }
                val dbio = for {
                  _ <- updateBalanceDbio(accountId, diff)
                  _ <- saveOffsetDbio(uuid)
                } yield Done
                db.run(dbio.transactionally)
            }
        }
      }

      val sink = Sink.foreach(println)

      val runnableGraph: RunnableGraph[Future[Done]] =
        source.via(flow).toMat(sink)(Keep.right)

      runnableGraph.run()
    }
  }

  private def fetchOffsetDbio()(
      implicit ec: ExecutionContext,
  ): DBIO[Option[TimeBasedUUID]] = {
    Tables.ReadModelUpdaterOffset
      .filter(_.tag === tag.bind)
      .map(_.offset)
      .result
      .headOption
      .map(_.map(uuid => TimeBasedUUID(UUID.fromString(uuid))))
  }

  private def saveOffsetDbio(offset: UUID): DBIO[Int] = {
    val row = Tables.ReadModelUpdaterOffsetRow(tag, offset.toString)
    Tables.ReadModelUpdaterOffset.insertOrUpdate(row)
  }

  private def updateBalanceDbio(accountId: AccountId, diff: Amount)(
      implicit ec: ExecutionContext,
  ): DBIO[Done] = {
    val balanceQuery = Tables.Balances
      .filter(_.accountId === accountId.value.bind)
      .map(_.amount.mapTo[Balance])

    val insertOrUpdateDbio = for {
      balanceOpt <- balanceQuery.result.headOption
      _ <- balanceOpt match {
        case Some(balance) =>
          balanceQuery.update(balance + diff)
        case None =>
          val balancesRow = Tables.BalancesRow(accountId.value, diff.value)
          Tables.Balances += balancesRow
      }

    } yield Done

    insertOrUpdateDbio.transactionally
  }
}

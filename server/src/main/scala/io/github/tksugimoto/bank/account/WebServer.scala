package io.github.tksugimoto.bank.account

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.persistence.{PersistentActor, RecoveryCompleted}
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import scala.util.{Failure, Success}

object WebServer {
  def main(args: Array[String]) {
    implicit val system: ActorSystem = ActorSystem("ClusterSystem")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    implicit val timeout: Timeout = Timeout.create(
      system.settings.config
        .getDuration("io.github.tksugimoto.bank.account.processing-timeout"),
    )

    warmUp()
    val accountShardRegion = Account.Sharding.startClusterSharding()

    val route =
      pathPrefix("account" / LongNumber) { rawAccountId: Long =>
        val accountId = AccountId(rawAccountId)
        concat(
          path("balance") {
            get {
              onSuccess(
                (accountShardRegion ? Account.GetBalance(accountId))
                  .mapTo[Balance],
              ) { balance =>
                println(s"[$accountId] $balance")
                complete(balance.toString)
              }
            }
          },
          path("deposit") {
            post {
              parameter("amount".as[Int]) { amount =>
                println(s"[$accountId] +$amount")
                onSuccess(
                  (accountShardRegion ? Account.Deposit(accountId, amount))
                    .mapTo[Done],
                ) { _: Done =>
                  complete("ok")
                }
              }
            }
          },
          path("withdraw") {
            post {
              parameter("amount".as[Int]) { amount =>
                println(s"[$accountId] -$amount")
                onComplete(
                  (accountShardRegion ? Account.Withdraw(accountId, amount))
                    .mapTo[Done],
                ) {
                  case Success(Done) => complete("ok")
                  case Failure(ex) =>
                    complete(StatusCodes.BadRequest -> ex.getMessage)

                }
              }
            }
          },
        )
      }

    val interface = system.settings.config.getString("http.interface")
    val port = system.settings.config.getInt("http.port")
    val bindingFuture = Http().bindAndHandle(route, interface, port)

    println(s"""
        |Example page: http://$interface:$port/account/123/balance
        |Press RETURN to stop...
      """.stripMargin)

    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

  def warmUp()(implicit system: ActorSystem): Unit = {
    // cassandraへの初回接続に時間がかかるため起動時に接続する
    system.actorOf(
      Props(new PersistentActor with ActorLogging {
        log.info("warming up started")
        override def receiveRecover: Receive = {
          case RecoveryCompleted =>
            log.info("warming up completed")
            context.stop(self)
        }

        override def receiveCommand: Receive = Actor.emptyBehavior

        override def persistenceId: String = "warming-up"
      }),
      name = "warming-up",
    )
  }
}

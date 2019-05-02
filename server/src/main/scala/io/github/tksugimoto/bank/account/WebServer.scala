package io.github.tksugimoto.bank.account

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import scala.util.{Failure, Success}

object WebServer {
  def main(args: Array[String]) {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    implicit val timeout: Timeout = Timeout.create(
      system.settings.config
        .getDuration("io.github.tksugimoto.bank.account.processing-timeout"),
    )

    Account.start()

    val route =
      pathPrefix("account" / LongNumber) { accountId: AccountId =>
        concat(
          path("balance") {
            get {
              onSuccess(Account.balance(accountId)) { balance =>
                println(s"[$accountId] $balance")
                complete(balance.toString)
              }
            }
          },
          path("deposit") {
            post {
              parameter("amount".as[Int]) { amount =>
                println(s"[$accountId] +$amount")
                onSuccess(Account.deposit(accountId, amount)) { _: Done =>
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
                  Account
                    .withdraw(accountId, amount),
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
}

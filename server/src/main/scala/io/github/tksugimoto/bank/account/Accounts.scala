package io.github.tksugimoto.bank.account

import akka.actor.{Actor, ActorRef, Props}

object Accounts {
  def props(): Props = Props(new Accounts())
}

class Accounts() extends Actor {
  override def receive: Receive = {
    case command: Account.Command =>
      val name = s"${command.accountId}"
      val accountActor = context.child(name).getOrElse(createAccountActor(name))
      accountActor forward command
  }

  def createAccountActor(name: String): ActorRef =
    context.actorOf(Account.props(), name)
}

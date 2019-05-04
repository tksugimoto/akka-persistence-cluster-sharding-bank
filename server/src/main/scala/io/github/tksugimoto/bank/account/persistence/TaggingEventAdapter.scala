package io.github.tksugimoto.bank.account.persistence

import akka.persistence.journal.{Tagged, WriteEventAdapter}
import io.github.tksugimoto.bank.account.Account

class TaggingEventAdapter extends WriteEventAdapter {
  override def manifest(event: Any): String = ""

  override def toJournal(event: Any): Any = event match {
    case event: Account.Event =>
      val tags = Set(AccountReadModelUpdater.tag)
      Tagged(event, tags)
    case _ => event
  }
}

package io.github.tksugimoto.bank.account.models
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = slick.jdbc.MySQLProfile
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema
      : profile.SchemaDescription = Balances.schema ++ ReadModelUpdaterOffset.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table Balances
    *  @param accountId Database column account_id SqlType(BIGINT), PrimaryKey
    *  @param amount Database column amount SqlType(INT) */
  case class BalancesRow(accountId: Long, amount: Int)

  /** GetResult implicit for fetching BalancesRow objects using plain SQL queries */
  implicit def GetResultBalancesRow(
      implicit e0: GR[Long],
      e1: GR[Int],
  ): GR[BalancesRow] = GR { prs =>
    import prs._
    BalancesRow.tupled((<<[Long], <<[Int]))
  }

  /** Table description of table balances. Objects of this class serve as prototypes for rows in queries. */
  class Balances(_tableTag: Tag)
      extends profile.api.Table[BalancesRow](
        _tableTag,
        Some("bank_read_model"),
        "balances",
      ) {
    def * = (accountId, amount) <> (BalancesRow.tupled, BalancesRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? =
      ((Rep.Some(accountId), Rep.Some(amount))).shaped.<>(
        { r =>
          import r._; _1.map(_ => BalancesRow.tupled((_1.get, _2.get)))
        },
        (_: Any) =>
          throw new Exception("Inserting into ? projection not supported."),
      )

    /** Database column account_id SqlType(BIGINT), PrimaryKey */
    val accountId: Rep[Long] = column[Long]("account_id", O.PrimaryKey)

    /** Database column amount SqlType(INT) */
    val amount: Rep[Int] = column[Int]("amount")
  }

  /** Collection-like TableQuery object for table Balances */
  lazy val Balances = new TableQuery(tag => new Balances(tag))

  /** Entity class storing rows of table ReadModelUpdaterOffset
    *  @param tag Database column tag SqlType(VARCHAR), PrimaryKey, Length(255,true)
    *  @param offset Database column offset SqlType(VARCHAR), Length(255,true) */
  case class ReadModelUpdaterOffsetRow(tag: String, offset: String)

  /** GetResult implicit for fetching ReadModelUpdaterOffsetRow objects using plain SQL queries */
  implicit def GetResultReadModelUpdaterOffsetRow(
      implicit e0: GR[String],
  ): GR[ReadModelUpdaterOffsetRow] = GR { prs =>
    import prs._
    ReadModelUpdaterOffsetRow.tupled((<<[String], <<[String]))
  }

  /** Table description of table read_model_updater_offset. Objects of this class serve as prototypes for rows in queries. */
  class ReadModelUpdaterOffset(_tableTag: Tag)
      extends profile.api.Table[ReadModelUpdaterOffsetRow](
        _tableTag,
        Some("bank_read_model"),
        "read_model_updater_offset",
      ) {
    def * =
      (tag, offset) <> (ReadModelUpdaterOffsetRow.tupled, ReadModelUpdaterOffsetRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? =
      ((Rep.Some(tag), Rep.Some(offset))).shaped.<>(
        { r =>
          import r._;
          _1.map(_ => ReadModelUpdaterOffsetRow.tupled((_1.get, _2.get)))
        },
        (_: Any) =>
          throw new Exception("Inserting into ? projection not supported."),
      )

    /** Database column tag SqlType(VARCHAR), PrimaryKey, Length(255,true) */
    val tag: Rep[String] =
      column[String]("tag", O.PrimaryKey, O.Length(255, varying = true))

    /** Database column offset SqlType(VARCHAR), Length(255,true) */
    val offset: Rep[String] =
      column[String]("offset", O.Length(255, varying = true))
  }

  /** Collection-like TableQuery object for table ReadModelUpdaterOffset */
  lazy val ReadModelUpdaterOffset = new TableQuery(
    tag => new ReadModelUpdaterOffset(tag),
  )
}

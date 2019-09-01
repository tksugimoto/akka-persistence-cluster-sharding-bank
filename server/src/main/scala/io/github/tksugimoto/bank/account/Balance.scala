package io.github.tksugimoto.bank.account

final case class Balance(value: Int) extends AnyVal {
  def +(amount: Int): Balance = copy(value + amount)
  def -(amount: Int): Balance = copy(value - amount)

  def isNegative: Boolean = value < 0
}

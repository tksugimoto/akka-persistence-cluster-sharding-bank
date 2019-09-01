package io.github.tksugimoto.bank.account

final case class Balance(value: Int) extends AnyVal {
  def +(amount: Amount): Balance = copy(value + amount.value)
  def -(amount: Amount): Balance = copy(value - amount.value)

  def isNegative: Boolean = value < 0
}

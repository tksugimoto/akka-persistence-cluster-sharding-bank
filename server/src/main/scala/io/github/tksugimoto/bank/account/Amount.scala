package io.github.tksugimoto.bank.account

final case class Amount(value: Int) extends AnyVal {
  def unary_+ : Amount = this
  def unary_- : Amount = copy(-value)
}

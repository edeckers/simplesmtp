package io.deckers.smtpjer.parsers

import arrow.core.Either

class EmailAddress(val address: String) {
  val hostname = address.split("@").last()
  val mailbox = address.substring(0, address.indexOf("@"))

  companion object {
    fun parse(address: String): Either<Throwable, EmailAddress> {
      if (address.length <= 3) {
        return Either.Left(Error("E-mail address length should be at least three character long"))
      }

      if (!address.matches("[a-zA-Z0-9.@-]+".toRegex())) {
        return Either.Left(Error("E-mail address contains unexpected characters"))
      }

      if (!address.contains('@')) {
        return Either.Left(Error("E-mail address must contain @"))
      }

      return Either.Right(EmailAddress(address))
    }
  }

  override fun toString(): String {
    return address
  }
}
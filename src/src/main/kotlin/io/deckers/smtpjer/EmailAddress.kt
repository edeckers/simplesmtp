package io.deckers.smtpjer

import arrow.core.Either

class EmailAddress(private val address: String) {
  val hostname = address.split("@")
  val mailbox = address.substring(0, address.indexOf("@"))

  companion object {
    fun parse(address: String): Either<Throwable, EmailAddress> =
      Either.Right(EmailAddress(address)) // FIXME ED Validate
  }

  override fun toString(): String {
    return address
  }
}
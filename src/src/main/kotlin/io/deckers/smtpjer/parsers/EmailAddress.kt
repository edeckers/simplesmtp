package io.deckers.smtpjer.parsers

import arrow.core.Either
import arrow.core.flatMap

class EmailAddress(val mailbox: String, domain: DomainName) {
  val domainName = domain.name
  val address = "${mailbox}@${domainName}"

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

      val errorOrDomainName = DomainName.parse(address.split("@").last())

      return errorOrDomainName.flatMap { domainName ->
        val mailbox = address.substring(0, address.indexOf("@"))

        Either.Right(EmailAddress(mailbox, domainName))
      }
    }
  }

  override fun toString(): String = address
}
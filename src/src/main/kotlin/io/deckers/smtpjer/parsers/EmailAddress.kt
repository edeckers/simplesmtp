package io.deckers.smtpjer.parsers

import arrow.core.*

class EmailAddress(val mailbox: String, domain: DomainName) {
  val domainName = domain.name
  val address = "${mailbox}@${domainName}"

  companion object {
    fun parse(address: String): Either<Throwable, EmailAddress> = Either.Right(address)
      .map { it.trim('<') }
      .map { it.trim('>') }
      .filterOrElse(
        { address.length > 3 },
        { Error("E-mail address length should be at least three character long") })
      .filterOrElse(
        {it.matches("[a-zA-Z0-9.@-]+".toRegex()) },
        { Error("E-mail address `$address` contains unexpected characters") })
      .filterOrElse(
        { address.contains('@') },
        { Error("E-mail address must contain @") })
      .flatMap {
        DomainName
          .parse(it.split("@").last())
          .map { domainName -> Pair(it, domainName) }
      }
      .map {
        val (trimmedAddress, domainName) = it
        val mailbox = trimmedAddress.substring(0, trimmedAddress.indexOf("@"))

        EmailAddress(mailbox, domainName)
      }
  }

  override fun toString(): String = address
}
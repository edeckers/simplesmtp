package io.deckers.smtpjer.parsers

import arrow.core.filterOrElse
import arrow.core.flatMap
import arrow.core.right

private fun stripBrackets(it: String) = it.trim('<').trim('>')
private fun containsAt(it: String) = it.contains('@')
private fun containsValidCharacters(it: String) = it.matches("[a-zA-Z0-9.@-]+".toRegex())
private fun hasMinLength(it: String) = it.length >= 3

@Suppress("MoveLambdaOutsideParentheses")
class EmailAddress(val mailbox: String, domain: DomainName) {
  val domainName = domain.name
  val address = "${mailbox}@${domainName}"

  companion object {
    fun parse(address: String) =
      address
        .right()
        .map(::stripBrackets)
        .filterOrElse(
          ::hasMinLength,
          { Error("E-mail address length should be at least three characters long") })
        .filterOrElse(
          ::containsValidCharacters,
          { Error("E-mail address `$address` contains unexpected characters") })
        .filterOrElse(
          ::containsAt,
          { Error("E-mail address `$address` does not contain @") })
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
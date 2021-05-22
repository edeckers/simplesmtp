package io.deckers.smtpjer.parsers

import arrow.core.filterOrElse
import arrow.core.right

private fun startsWithAlphaNumeric(it: String) = it.matches("[a-zA-Z0-9].*".toRegex())
private fun containsValidCharacters(it: String) = it.matches("[a-zA-Z0-9.-]+".toRegex())

@Suppress("MoveLambdaOutsideParentheses")
class DomainName(val name: String) {
  companion object {
    fun parse(address: String) =
      address
        .right()
        .filterOrElse(
          String::isNotEmpty,
          { Error("Domain name cannot be empty") }
        )
        .filterOrElse(
          ::containsValidCharacters,
          { Error("Domain name '$address' contains unexpected characters") }
        )
        .filterOrElse(
          ::startsWithAlphaNumeric,
          { Error("Domain name '$address' must start with alphanumeric character") }
        )
        .map(::DomainName)
  }

  override fun toString(): String = name
}
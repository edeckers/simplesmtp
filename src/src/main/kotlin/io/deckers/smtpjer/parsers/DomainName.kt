package io.deckers.smtpjer.parsers

import arrow.core.Either

class DomainName(val name: String) {
  companion object {
    fun parse(address: String): Either<Throwable, DomainName> {
      if (address.isEmpty()) {
        return Either.Left(Error("Domain name cannot be empty"))
      }

      if (!address.matches("[a-zA-Z0-9.-]+".toRegex())) {
        return Either.Left(Error("Domain name contains unexpected characters"))
      }

      if (!address.matches("[a-zA-Z0-9].*".toRegex())) {
        return Either.Left(Error("Domain name must start with alphanumeric character"))
      }

      return Either.Right(DomainName(address))
    }
  }

  override fun toString(): String = name
}
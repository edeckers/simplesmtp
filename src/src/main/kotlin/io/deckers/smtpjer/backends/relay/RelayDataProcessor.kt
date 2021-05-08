package io.deckers.smtpjer.backends.relay

import io.deckers.smtpjer.parsers.EmailAddress
import io.deckers.smtpjer.backends.DataProcessor
import io.deckers.smtpjer.parsers.DomainName

class RelayDataProcessor(
  domain: DomainName,
  from: EmailAddress,
  to: EmailAddress
) : DataProcessor {
  override fun write(line: String) = Unit

  override fun close() {
  }
}
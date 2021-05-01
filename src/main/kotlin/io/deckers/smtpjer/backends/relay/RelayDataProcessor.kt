package io.deckers.smtpjer.backends.relay

import io.deckers.smtpjer.EmailAddress
import io.deckers.smtpjer.backends.DataProcessor

class RelayDataProcessor(
  domain: String,
  from: EmailAddress,
  to: EmailAddress
) : DataProcessor {
  override fun write(line: String) = Unit

  override fun close() {
  }
}
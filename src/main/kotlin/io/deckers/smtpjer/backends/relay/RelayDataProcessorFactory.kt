package io.deckers.smtpjer.backends.relay

import io.deckers.smtpjer.EmailAddress
import io.deckers.smtpjer.backends.DataProcessor
import io.deckers.smtpjer.backends.DataProcessorFactory

class RelayDataProcessorFactory : DataProcessorFactory {
  override fun create(domain: String, from: EmailAddress, to: EmailAddress): DataProcessor =
    RelayDataProcessor(domain, from, to)
}
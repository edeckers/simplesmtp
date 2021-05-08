package io.deckers.smtpjer.backends.relay

import io.deckers.smtpjer.parsers.EmailAddress
import io.deckers.smtpjer.backends.DataProcessor
import io.deckers.smtpjer.backends.DataProcessorFactory
import io.deckers.smtpjer.parsers.DomainName

class RelayDataProcessorFactory : DataProcessorFactory {
  override fun create(domain: DomainName, from: EmailAddress, to: EmailAddress): DataProcessor =
    RelayDataProcessor(domain, from, to)
}
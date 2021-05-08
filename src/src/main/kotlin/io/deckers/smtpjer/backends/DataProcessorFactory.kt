package io.deckers.smtpjer.backends

import io.deckers.smtpjer.parsers.DomainName
import io.deckers.smtpjer.parsers.EmailAddress

interface DataProcessorFactory {
  fun create(domain: DomainName, from: EmailAddress, to: EmailAddress) : DataProcessor
}
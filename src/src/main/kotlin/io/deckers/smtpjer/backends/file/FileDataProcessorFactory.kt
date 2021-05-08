package io.deckers.smtpjer.backends.file

import io.deckers.smtpjer.parsers.EmailAddress
import io.deckers.smtpjer.backends.DataProcessor
import io.deckers.smtpjer.backends.DataProcessorFactory
import io.deckers.smtpjer.parsers.DomainName

class FileDataProcessorFactory : DataProcessorFactory {
  override fun create(domain: DomainName, from: EmailAddress, to: EmailAddress): DataProcessor =
    FileDataProcessor(to)
}
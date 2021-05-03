package io.deckers.smtpjer.backends.file

import io.deckers.smtpjer.parsers.EmailAddress
import io.deckers.smtpjer.backends.DataProcessor
import io.deckers.smtpjer.backends.DataProcessorFactory

class FileDataProcessorFactory : DataProcessorFactory {
  override fun create(domain: String, from: EmailAddress, to: EmailAddress): DataProcessor =
    FileDataProcessor(to)
}
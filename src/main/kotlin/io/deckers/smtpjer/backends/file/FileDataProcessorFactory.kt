package io.deckers.smtpjer.backends.file

import io.deckers.smtpjer.backends.DataProcessor
import io.deckers.smtpjer.backends.DataProcessorFactory

class FileDataProcessorFactory : DataProcessorFactory {
  override fun create(domain: String, from: String, to: String): DataProcessor = FileDataProcessor(domain, from, to)
}
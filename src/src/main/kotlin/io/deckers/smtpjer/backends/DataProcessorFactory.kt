package io.deckers.smtpjer.backends

import io.deckers.smtpjer.EmailAddress

interface DataProcessorFactory {
  fun create(domain: String, from: EmailAddress, to: EmailAddress) : DataProcessor
}
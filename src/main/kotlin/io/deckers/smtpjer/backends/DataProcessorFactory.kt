package io.deckers.smtpjer.backends

interface DataProcessorFactory {
  fun create(domain: String, from: String, to: String) : DataProcessor
}
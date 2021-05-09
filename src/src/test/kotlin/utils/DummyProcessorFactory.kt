import io.deckers.smtpjer.backends.DataProcessor
import io.deckers.smtpjer.backends.DataProcessorFactory
import io.deckers.smtpjer.parsers.DomainName
import io.deckers.smtpjer.parsers.EmailAddress

class DummyProcessor : DataProcessor {
  override fun write(line: String) {
    /* noop */
  }

  override fun close() {
    /* noop */
  }
}

class DummyProcessorFactory : DataProcessorFactory {
  override fun create(domain: DomainName, from: EmailAddress, to: EmailAddress): DataProcessor = DummyProcessor()
}
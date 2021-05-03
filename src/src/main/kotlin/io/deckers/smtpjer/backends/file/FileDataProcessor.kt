package io.deckers.smtpjer.backends.file

import io.deckers.smtpjer.parsers.EmailAddress
import io.deckers.smtpjer.backends.DataProcessor
import java.io.File

class FileDataProcessor(to: EmailAddress) : DataProcessor {
  private val os = File.createTempFile("smtp-ely.${to.mailbox}.", ".txt").outputStream()

  override fun write(line: String) {
    os.write(line.toByteArray())
  }

  override fun close() {
    os.close()
  }
}
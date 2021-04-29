package io.deckers.smtpjer.backends.file

import io.deckers.smtpjer.backends.DataProcessor
import java.io.File

class FileDataProcessor(domain: String, from: String, to: String) : DataProcessor {
  private val os = File.createTempFile("smtp-ely.$domain.", ".txt").outputStream()

  override fun write(line: String) {
    os.write(line.toByteArray())
  }

  override fun close() {
    os.close()
  }
}
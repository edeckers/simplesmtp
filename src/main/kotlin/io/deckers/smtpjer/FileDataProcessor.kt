package io.deckers.smtpjer

import java.io.File
import java.io.OutputStream

class FileDataProcessor : DataProcessor {
  override fun create(domain: String, from: String, to: String): OutputStream =
    File.createTempFile("smtp-ely", "txt").outputStream()
}
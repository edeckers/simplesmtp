package io.deckers.smtpjer.backends

import java.io.Closeable

interface DataProcessor : Closeable {
  fun write(line: String)
}
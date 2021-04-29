package io.deckers.smtpjer

import java.io.OutputStream

interface DataProcessor {
  fun create(domain: String, from: String, to: String) : OutputStream
}
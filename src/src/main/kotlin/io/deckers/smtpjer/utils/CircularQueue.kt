package io.deckers.smtpjer.utils

import java.util.*

class CircularQueue(private val numElements: Int) : ArrayDeque<String>(numElements) {
  override fun push(element: String) {
    if (size == numElements) {
      removeLast()
    }

    super.push(element)
  }
}

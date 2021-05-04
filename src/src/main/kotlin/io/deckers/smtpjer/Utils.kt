package io.deckers.smtpjer

import java.util.*

fun <T> List<T>.destructure() = Pair(component1(), drop(1))

class CircularQueue(private val numElements: Int) : ArrayDeque<String>(numElements) {
  override fun push(element: String) {
    if (size == numElements) {
      removeLast()
    }

    super.push(element)
  }
}

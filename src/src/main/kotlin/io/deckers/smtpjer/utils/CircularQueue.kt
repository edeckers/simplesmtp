/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
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

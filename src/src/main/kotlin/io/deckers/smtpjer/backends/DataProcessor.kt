/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.smtpjer.backends

import java.io.Closeable

interface DataProcessor : Closeable {
  fun write(line: String)
}
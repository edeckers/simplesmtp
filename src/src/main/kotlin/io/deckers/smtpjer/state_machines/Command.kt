/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.smtpjer.state_machines

sealed class Command {
  object ReceiveData : Command()
  object Quit : Command()

  data class WriteStatus(val code: Int, val message: String, val extendedCode: String? = null) : Command()
}
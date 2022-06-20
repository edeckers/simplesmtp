/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.smtpjer.state_machines

import io.deckers.smtpjer.parsers.DomainName
import io.deckers.smtpjer.parsers.EmailAddress

sealed class State {
  object Start : State()
  object Helo : State()
  data class MailFrom(val domain: DomainName) : State()
  data class RcptTo(val domain: DomainName, val mailFrom: EmailAddress) : State()
  data class Data(val domain: DomainName, val mailFrom: EmailAddress, val rcptTo: EmailAddress) : State()
}
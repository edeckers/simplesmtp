/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.smtpjer.state_machines

import io.deckers.smtpjer.parsers.DomainName
import io.deckers.smtpjer.parsers.EmailAddress

sealed class Event {
  object OnConnect : Event()
  object OnData : Event()
  data class OnEhlo(val domain: DomainName) : Event()
  data class OnHelo(val domain: DomainName) : Event()
  data class OnMailFrom(val emailAddress: EmailAddress) : Event()
  object OnParseError : Event()
  object OnQuit : Event()
  data class OnRcptTo(val emailAddress: EmailAddress) : Event()
}
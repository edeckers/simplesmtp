package io.deckers.smtpjer.state_machine

import io.deckers.smtpjer.parsers.EmailAddress

sealed class Event {
  object OnConnect : Event()
  object OnData : Event()
  data class OnEhlo(val domain: String) : Event()
  data class OnHelo(val domain: String) : Event()
  data class OnMailFrom(val emailAddress: EmailAddress) : Event()
  object OnParseError : Event()
  object OnQuit : Event()
  data class OnRcptTo(val emailAddress: EmailAddress) : Event()
}
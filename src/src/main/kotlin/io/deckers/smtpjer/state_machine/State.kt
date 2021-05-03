package io.deckers.smtpjer.state_machine

import io.deckers.smtpjer.parsers.EmailAddress

sealed class State {
  object Start : State()
  object Helo : State()
  data class MailFrom(val domain: String) : State()
  data class RcptTo(val domain: String, val mailFrom: EmailAddress) : State()
  data class Data(val domain: String, val rcptTo: EmailAddress, val mailFrom: EmailAddress) : State()
  object Finish : State()
}
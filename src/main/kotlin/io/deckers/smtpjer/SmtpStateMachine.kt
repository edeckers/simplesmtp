package io.deckers.smtpjer

sealed class State {
  object Ehlo : State()
  data class MailFrom(val domain: String) : State()
  data class RcptTo(val domain: String, val mailFrom: String) : State()
  data class Data(val domain: String, val rcptTo: String, val mailFrom: String) : State()
}

sealed class Event {
  data class OnEhlo(val domain: String) : Event()
  data class OnMailFrom(val emailAddress: String) : Event()
  data class OnRcptTo(val emailAddress: String) : Event()
  object OnData : Event()
  object OnDataCompleted : Event()
}

sealed class Command {
  object ReceiveData : Command()
}
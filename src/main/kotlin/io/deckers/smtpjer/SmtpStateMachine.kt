package io.deckers.smtpjer

sealed class State {
  object Start : State()
  object Helo : State()
  data class MailFrom(val domain: String) : State()
  data class RcptTo(val domain: String, val mailFrom: String) : State()
  data class Data(val domain: String, val rcptTo: String, val mailFrom: String) : State()
  object Finish : State()
}

sealed class Event {
  object OnConnect : Event()
  object OnData : Event()
  data class OnEhlo(val domain: String) : Event()
  data class OnHelo(val domain: String) : Event()
  data class OnMailFrom(val emailAddress: String) : Event()
  object OnParseError : Event()
  object OnQuit : Event()
  data class OnRcptTo(val emailAddress: String) : Event()
}

sealed class Command {
  object ReceiveData : Command()
  data class WriteStatus(val code: Int, val message: String) : Command()
}
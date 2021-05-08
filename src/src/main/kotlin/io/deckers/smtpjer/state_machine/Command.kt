package io.deckers.smtpjer.state_machine

sealed class Command {
  object ReceiveData : Command()
  object Quit : Command()

  data class WriteStatus(val code: Int, val message: String) : Command()
}
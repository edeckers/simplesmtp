package io.deckers.smtpjer.state_machines

sealed class Command {
  object ReceiveData : Command()
  object Quit : Command()

  data class WriteStatus(val code: Int, val message: String) : Command()
}
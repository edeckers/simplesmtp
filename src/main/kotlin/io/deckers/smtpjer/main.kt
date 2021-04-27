package io.deckers.smtpjer

import kotlin.system.exitProcess

fun main() {
  val s = SmtpServer(9999)

  s.use {
    println("Press any key to quit.")
    readLine()
  }

  exitProcess(0)
}
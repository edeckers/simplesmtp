package io.deckers.smtpjer

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlin.system.exitProcess

private const val DefaultPort = 25

fun main(args: Array<String>) {
  val argumentParser = ArgParser("KtSmtp")

  val port by argumentParser
    .option(
      ArgType.Int,
      shortName = "p",
      fullName = "port",
      description = "What port should the server run on?"
    )
    .default(DefaultPort)

  argumentParser.parse(args)

  SmtpServer(port).use {
    println("Press any key to quit.")
    readLine()
  }

  exitProcess(0)
}
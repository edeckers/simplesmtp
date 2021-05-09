package io.deckers.smtpjer

import io.deckers.smtpjer.backends.proxy.PoorMansProxyFactory
import io.deckers.smtpjer.services.SmtpServer
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.net.InetAddress
import kotlin.system.exitProcess

private const val DefaultPort = 25

fun main(args: Array<String>) {
  val argumentParser = ArgParser("KtSmtp")

  val dataProcessorFactory =
    PoorMansProxyFactory(
      InetAddress.getByName("mail.deckers.io"),
      DefaultPort
    )

  val port by argumentParser
    .option(
      ArgType.Int,
      shortName = "p",
      fullName = "port",
      description = "What port should the server run on?"
    )
    .default(DefaultPort)

  argumentParser.parse(args)

  SmtpServer(port, dataProcessorFactory).run().use {
    println("Press any key to quit.")
    readLine()
  }

  exitProcess(0)
}
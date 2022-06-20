/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.smtpjer

import io.deckers.smtpjer.backends.file.FileDataProcessorFactory
import io.deckers.smtpjer.backends.proxy.PoorMansProxyFactory
import io.deckers.smtpjer.services.SmtpServer
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import mu.KotlinLogging
import java.net.InetAddress
import kotlin.system.exitProcess

private enum class Backends {
  File,
  Proxy
}

private val backendOptions = listOf(Backends.File, Backends.Proxy)

private const val DefaultPort = 25
private val DefaultBackend = Backends.File

private val logger = KotlinLogging.logger {}

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

  val backend by argumentParser
    .option(
      ArgType.Choice(
        backendOptions,
        { Backends.values().first { v -> v.name.equals(it, ignoreCase = true) } },
        { it.toString().toLowerCase() }),
      shortName = "b",
      fullName = "backend",
      description = "What backend should be used?"
    )
    .default(DefaultBackend)

  val maybeProxyTo by argumentParser
    .option(
      ArgType.String,
      shortName = "t",
      fullName = "proxy_to",
      description = "What is the proxy host. For usage with backend 'proxy'"
    )

  val proxyPort by argumentParser
    .option(
      ArgType.Int,
      shortName = "o",
      fullName = "proxy_port",
      description = "On what port is the proxy host listening?"
    )
    .default(DefaultPort)

  argumentParser.parse(args)

  logger.info("Starting server on port $port")
  logger.info("Using '$backend' backend")

  if (backend == Backends.Proxy) {
    if (maybeProxyTo == null) {
      logger.info("No proxy host was provided; it's required when using backend 'proxy'")
      return
    }

    logger.info("Proxying requests to $maybeProxyTo:$proxyPort")
  }

  val dataProcessorFactory = when (backend) {
    Backends.File -> FileDataProcessorFactory()
    Backends.Proxy -> PoorMansProxyFactory(
      InetAddress.getByName(maybeProxyTo),
      DefaultPort
    )
  }

  SmtpServer(port, dataProcessorFactory).run().use {
    println("Press any key to quit.")
    readLine()
  }

  exitProcess(0)
}
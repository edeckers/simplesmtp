package io.deckers.smtpjer.backends.proxy

import io.deckers.smtpjer.parsers.EmailAddress
import io.deckers.smtpjer.backends.DataProcessor
import io.deckers.smtpjer.parsers.DomainName
import mu.KotlinLogging
import java.net.InetAddress
import java.net.Socket
import java.util.*

class PoorMansProxy(
  host: InetAddress,
  port: Int,
  domain: DomainName,
  from: EmailAddress,
  to: EmailAddress
) : DataProcessor {
  private val logger = KotlinLogging.logger {}

  private val client = Socket(host, port)
  private val writer = client.getOutputStream()
  private val reader = Scanner(client.getInputStream())

  init {
    logger.debug(reader.nextLine())
    writer.write("HELO ${domain.name}\n".toByteArray())
    logger.debug(reader.nextLine())
    writer.write("MAIL FROM: ${from.address}\n".toByteArray())
    logger.debug(reader.nextLine())
    writer.write("RCPT TO: ${to.address}\n".toByteArray())
    logger.debug(reader.nextLine())
    writer.write("DATA\n".toByteArray())
    logger.debug(reader.nextLine())
  }

  override fun write(line: String) {
    writer.write("$line\n".toByteArray())
    logger.debug("Relaying $line")
  }

  override fun close() {
    logger.debug("Closing ${PoorMansProxy::class.simpleName}")
    writer.write("QUIT\n".toByteArray())
    writer.close()
    client.close()
    logger.debug("Closed ${PoorMansProxy::class.simpleName}")
  }
}
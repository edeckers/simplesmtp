package io.deckers.smtpjer

import mu.KotlinLogging
import java.io.Closeable
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

// ehlo example.com
// mail from: ely@infi.nl
// rcpt to: ely@deckers.io
// data
// Subject: My Telnet Test Email
//
// Hello,
//
// This is an email sent by using the telnet command.
//
// Your friend,
// Me
//
// .


// EHLO <domain>
// MAIL FROM: <e-mail address>
// RCPT TO: <e-mail address>
// DATA
// ...
//
// .

// expr      := <ehlo> | <mail_from> | <rcpt_to> | <data>
// ehlo      := ehlo <domain_name>
// mail_from := mail from: <email_address>
// rcpt_to   := rcpt to: <email_address>
// data      := binary <crlf><crlf>.

const val COMMAND_EHLO = "EHLO"
const val COMMAND_MAIL_FROM = "MAIL FROM:"
const val COMMAND_RCPT_TO = "RCPT TO:"
const val COMMAND_DATA = "DATA"

const val STATE_EHLO = "STATE_EHLO"
const val STATE_RCPT_TO = "STATE_MAIL_TO"
const val STATE_MAIL_FROM = "STATE_MAIL_FROM"
const val STATE_DATA = "STATE_DATA"

val stateTable = mapOf(
  STATE_EHLO to mapOf(COMMAND_EHLO to STATE_RCPT_TO),
  STATE_MAIL_FROM to mapOf(COMMAND_MAIL_FROM to STATE_RCPT_TO),
  STATE_RCPT_TO to mapOf(COMMAND_RCPT_TO to STATE_DATA),
  STATE_DATA to mapOf(COMMAND_DATA to STATE_EHLO),
)

private class SmtpClientHandler(client: Socket) {
  private val reader: Scanner = Scanner(client.getInputStream())
  private val smtpStateMachine = SmtpStateMachine()

  fun run() {
    while (true) {
      val errorOrParsedCommand = parse(reader.nextLine())

      val process =
        errorOrParsedCommand
          .fold(
            { e -> { logger.error(e) { "Nou zeg." } } },
            { cmd ->
              {
                smtpStateMachine.next(cmd[0], cmd.drop(1))
              }
            }
          )

      process()
    }
  }
}

class SmtpServer(port: Int) : Closeable {
  private val server = ServerSocket(port)

  init {
    logger.info("Server listening (port={})", server.localPort)

    thread {
      val client = server.accept()
      logger.info("Client connected (host={})", client.inetAddress.hostAddress)

      SmtpClientHandler(client).run()
    }

    logger.info("OH.")
  }

  override fun close() {
    if (server.isClosed) {
      return
    }

    server.close()
  }
}
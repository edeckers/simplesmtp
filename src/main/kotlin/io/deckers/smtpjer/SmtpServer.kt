package io.deckers.smtpjer

import com.tinder.StateMachine
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
const val COMMAND_MAIL_FROM = "MAIL FROM"
const val COMMAND_RCPT_TO = "RCPT TO"
const val COMMAND_DATA = "DATA"

private class SmtpClientHandler(client: Socket) {
  private val reader: Scanner = Scanner(client.getInputStream())

  private val stateMachine = StateMachine.create<State, Event, Command> {
    initialState(State.Ehlo)

    state<State.Ehlo> {
      on<Event.OnEhlo> {
        transitionTo(State.MailFrom(it.domain))
      }
    }

    state<State.MailFrom> {
      on<Event.OnMailFrom> {
        transitionTo(State.RcptTo(domain, it.emailAddress))
      }
    }

    state<State.RcptTo> {
      on<Event.OnRcptTo> {
        transitionTo(State.Data(domain, mailFrom, it.emailAddress))
      }
    }

    state<State.Data> {
      on<Event.OnData> {
        transitionTo(State.Data(domain, mailFrom, rcptTo), Command.ReceiveData)
      }
      on<Event.OnDataCompleted> {
        transitionTo(State.Ehlo)
      }
    }

    onTransition {
      val validTransition = it as? StateMachine.Transition.Valid
      if (validTransition == null) {
        logger.error("Failed to change state {} using command {}", it.fromState, it.event)
        return@onTransition
      }

      when (val evt = validTransition.event) {
        is Event.OnEhlo -> logger.debug("EHLO {}", evt.domain)
        is Event.OnMailFrom -> logger.debug("MAIL FROM {}", evt.emailAddress)
        is Event.OnRcptTo -> logger.debug("RCPT TO {}", evt.emailAddress)
        is Event.OnData -> logger.debug("DATA")
      }

      when (validTransition.sideEffect) {
        Command.ReceiveData -> {
          logger.debug("Started data retrieval")

          var line = ""
          while (line != ".") {
            line = reader.nextLine()
            logger.debug { line }
          }

          logger.debug("Finished data retrieval")
        }
      }
    }
  }

  fun run() {
    while (true) {
      val errorOrParsedCommand = parse(reader.nextLine())

      val process =
        errorOrParsedCommand
          .fold(
            { e -> { logger.error(e) { "Failed to parse command" }; stateMachine.transition(Event.OnParseError) } },
            { o -> { stateMachine.transition(o) } }
          )

      val transaction = process()

      if (transaction is StateMachine.Transition.Invalid) {
        break
      }
    }
  }
}

class SmtpServer(port: Int) : Closeable {
  private val server = ServerSocket(port)

  init {
    logger.info("Server listening (port={})", server.localPort)

    thread {
      while (true) {
        val client = server.accept()

        thread {
          client.use {
            logger.info("Client connected (host={})", it.inetAddress.hostAddress)

            SmtpClientHandler(it).run()
          }
        }
      }
    }
  }

  override fun close() {
    if (server.isClosed) {
      return
    }

    server.close()
  }
}
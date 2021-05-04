package io.deckers.smtpjer

import com.tinder.StateMachine
import io.deckers.smtpjer.backends.file.FileDataProcessorFactory
import io.deckers.smtpjer.parsers.parse
import io.deckers.smtpjer.state_machine.Command
import io.deckers.smtpjer.state_machine.Event
import io.deckers.smtpjer.state_machine.State
import mu.KotlinLogging
import java.io.Closeable
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

const val CommandData = "DATA"
const val CommandEhlo = "EHLO"
const val CommandHelo = "HELO"
const val CommandMailFrom = "MAIL FROM"
const val CommandRcptTo = "RCPT TO"
const val CommandQuit = "QUIT"

private val EndOfDataStreamPattern = arrayOf("", ".", "")

private class SmtpClientHandler(client: Socket) {
  private val reader: Scanner = Scanner(client.getInputStream())
  private val writer = client.getOutputStream()
  private val processorFactory = FileDataProcessorFactory()

  private val stateMachine = StateMachine.create<State, Event, Command> {
    initialState(State.Start)

    state<State.Start> {
      on<Event.OnConnect> {
        transitionTo(State.Helo, Command.WriteStatus(220, "${InetAddress.getLocalHost()} Service ready"))
      }
    }

    state<State.Helo> {
      on<Event.OnEhlo> {
        transitionTo(State.Helo, Command.WriteStatus(500, "5.5.1 Syntax error, command unrecognized"))
      }
      on<Event.OnHelo> {
        transitionTo(State.MailFrom(it.domain), Command.WriteStatus(250, "Ok"))
      }
    }

    state<State.MailFrom> {
      on<Event.OnMailFrom> {
        transitionTo(State.RcptTo(domain, it.emailAddress), Command.WriteStatus(250, "2.1.0 Ok"))
      }
    }

    state<State.RcptTo> {
      on<Event.OnRcptTo> {
        transitionTo(
          State.Data(domain, mailFrom, it.emailAddress), Command.WriteStatus(250, "2.1.5 Ok"),
        )
      }
    }

    state<State.Data> {
      on<Event.OnData> {
        transitionTo(State.Finish, Command.ReceiveData)
      }
      on<Event.OnParseError> {
        dontTransition(Command.WriteStatus(500, "Syntax error, command unrecognized"))
      }
    }

    state<State.Finish> {
      on<Event.OnQuit> {
        transitionTo(State.Helo)
      }
      on<Event.OnParseError> {
        dontTransition(Command.WriteStatus(500, "Syntax error, command unrecognized"))
      }
    }

    onTransition {
      val validTransition = it as? StateMachine.Transition.Valid
      if (validTransition == null) {
        logger.error("Failed to change state {} using command {}", it.fromState, it.event)
        return@onTransition
      }

      when (val evt = validTransition.event) {
        is Event.OnData -> logger.debug("DATA")
        is Event.OnEhlo -> logger.debug("EHLO {}", evt.domain)
        is Event.OnHelo -> logger.debug("HELO {}", evt.domain)
        is Event.OnMailFrom -> logger.debug("MAIL FROM {}", evt.emailAddress)
        is Event.OnRcptTo -> logger.debug("RCPT TO {}", evt.emailAddress)
        is Event.OnQuit -> logger.debug("QUIT")
      }

      when (val cmd = validTransition.sideEffect) {
        is Command.ReceiveData -> {
          writer.write("354 Start mail input; end with <CRLF>.<CRLF>\n".toByteArray())

          val st = validTransition.fromState as State.Data
          val processor = processorFactory.create(st.domain, st.mailFrom, st.rcptTo)

          logger.debug("Started data retrieval")

          val lastThreeLines = ArrayDeque<String>(3)
          while (!lastThreeLines.toArray().contentEquals(EndOfDataStreamPattern)) {
            val line = reader.nextLine()
            lastThreeLines.addLast(line)

            processor.write(line)
            logger.debug("Line: $line")
          }

          logger.debug("Finished data retrieval")

          writer.write("250 2.6.0 Message Accepted\n".toByteArray())
        }
        is Command.WriteStatus -> {
          writer.write("${cmd.code} ${cmd.message}\n".toByteArray())
        }
      }
    }
  }

  fun run() {
    var transaction = stateMachine.transition(Event.OnConnect)

    while (transaction is StateMachine.Transition.Valid) {
      val errorOrParsedCommand = parse(reader.nextLine())

      val process =
        errorOrParsedCommand
          .fold(
            { e -> { logger.error(e) { "Failed to parse command" }; stateMachine.transition(Event.OnParseError) } },
            { o -> { stateMachine.transition(o) } }
          )

      transaction = process()
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
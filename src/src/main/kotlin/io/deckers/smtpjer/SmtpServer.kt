package io.deckers.smtpjer

import arrow.core.Option
import arrow.core.getOrElse
import com.tinder.StateMachine
import io.deckers.smtpjer.backends.file.FileDataProcessorFactory
import io.deckers.smtpjer.parsers.parseCommand
import io.deckers.smtpjer.state_machine.Command
import io.deckers.smtpjer.state_machine.Event
import io.deckers.smtpjer.state_machine.State
import mu.KotlinLogging
import java.io.Closeable
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.*
import kotlin.NoSuchElementException
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

private val EndOfDataStreamPattern = arrayOf("", ".", "")

private class SmtpClientHandler(client: Socket) : Closeable {
  private val reader: Scanner = Scanner(client.getInputStream())
  private val writer = client.getOutputStream()
  private val processorFactory = FileDataProcessorFactory()

  private fun status(code: Int, message: String, extendedCode: String? = null): Command.WriteStatus =
    Command.WriteStatus(
      code,
      Option.fromNullable(extendedCode)
        .map { "$it $message" }
        .getOrElse { message })

  private fun <S : State> StateMachine.GraphBuilder<State, Event, Command>.StateDefinitionBuilder<S>.onEscalation() {
    on<Event.OnParseError> {
      dontTransition(status(500, "Syntax error, command unrecognized"))
    }
    on<Event.OnQuit> {
      dontTransition(Command.Quit)
    }
  }

  private val stateMachine = StateMachine.create<State, Event, Command> {
    initialState(State.Start)

    state<State.Start> {
      on<Event.OnConnect> {
        transitionTo(State.Helo, status(220, "${InetAddress.getLocalHost()} Service ready"))
      }

      onEscalation()
    }

    state<State.Helo> {
      on<Event.OnEhlo> {
        transitionTo(State.Helo, status(500, "Syntax error, command unrecognized", "5.5.1"))
      }
      on<Event.OnHelo> {
        transitionTo(State.MailFrom(it.domain), status(250, "Ok"))
      }

      onEscalation()
    }

    state<State.MailFrom> {
      on<Event.OnMailFrom> {
        transitionTo(State.RcptTo(domain, it.emailAddress), status(250, "Ok", "2.1.0"))
      }

      onEscalation()
    }

    state<State.RcptTo> {
      on<Event.OnRcptTo> {
        transitionTo(
          State.Data(domain, mailFrom, it.emailAddress), status(250, "Ok", "2.1.5"),
        )
      }

      onEscalation()
    }

    state<State.Data> {
      on<Event.OnData> {
        transitionTo(State.Helo, Command.ReceiveData)
      }

      onEscalation()
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
        is Command.Quit -> {
          writer.write("221 2.0.0 Bye\n".toByteArray())
          client.close()
        }
        is Command.ReceiveData -> {
          writer.write("354 Start mail input; end with <CRLF>.<CRLF>\n".toByteArray())

          val st = validTransition.fromState as State.Data
          val processor = processorFactory.create(st.domain, st.mailFrom, st.rcptTo)

          logger.debug("Started data retrieval")

          val lastThreeLines = CircularQueue(3)
          while (!lastThreeLines.toArray().contentEquals(EndOfDataStreamPattern)) {
            val line = reader.nextLine()
            lastThreeLines.push(line)

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

  private tailrec fun nextState() {
    try {
      val errorOrParsedCommand = parseCommand(reader.nextLine())

      val process =
        errorOrParsedCommand
          .fold(
            { e -> { logger.error(e) { "Failed to parse command" }; stateMachine.transition(Event.OnParseError) } },
            { o -> { stateMachine.transition(o) } }
          )

      process()
    } catch (e: NoSuchElementException) {
    }

    nextState()
  }

  fun run() {
    logger.debug("Running ${SmtpClientHandler::class.simpleName}")

    stateMachine.transition(Event.OnConnect)

    nextState()

    logger.debug("Ran ${SmtpClientHandler::class.simpleName}")
  }

  override fun close() {
    logger.debug("Closing ${SmtpClientHandler::class.simpleName}")
    reader.close()
    writer.close()
    logger.debug("Closed ${SmtpClientHandler::class.simpleName}")
  }
}

private fun runSmtpClientHandler(socket: Socket) = thread {
  logger.info("Client connected (host={})", socket.inetAddress.hostAddress)

  SmtpClientHandler(socket).use { handler ->
    handler.run()
  }

  logger.info("Client disconnected (host={})", socket.inetAddress.hostAddress)
}

class SmtpServer constructor(port: Int) : Closeable {
  private val socket = ServerSocket(port)

  override fun close() {
    if (socket.isClosed) {
      return
    }

    socket.close()
  }

  private tailrec fun waitForConnections() {
    try {
      logger.info("Waiting for connections on port (port={})", socket.localPort)
      val client = socket.accept()
      logger.info("Received connection on port (port={})", socket.localPort)

      runSmtpClientHandler(client)
    } catch (e: SocketException) {
      return
    }

    waitForConnections()
  }

  fun run(): SmtpServer {
    thread {
      waitForConnections()
    }

    return this
  }
}
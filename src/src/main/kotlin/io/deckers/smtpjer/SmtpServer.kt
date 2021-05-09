package io.deckers.smtpjer

import arrow.core.Either
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
import java.util.*
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

private val EndOfDataStreamPattern = arrayOf("", ".", "")

private fun logEvent(event: Event) =
  when (event) {
    is Event.OnData -> logger.debug("DATA")
    is Event.OnEhlo -> logger.debug("EHLO {}", event.domain)
    is Event.OnHelo -> logger.debug("HELO {}", event.domain)
    is Event.OnMailFrom -> logger.debug("MAIL FROM {}", event.emailAddress)
    is Event.OnRcptTo -> logger.debug("RCPT TO {}", event.emailAddress)
    is Event.OnQuit -> logger.debug("QUIT")
    else -> logger.error("Unknown event ${event::class.simpleName}")
  }

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

private class SmtpClientHandler(private val client: Socket) : Closeable {
  private val reader: Scanner = Scanner(client.getInputStream())
  private val writer = client.getOutputStream()
  private val processorFactory = FileDataProcessorFactory()

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

    onTransition { t ->
      logEvent(t.event);

      val handleTransition =
        Option.fromNullable(t as? StateMachine.Transition.Valid)
          .map(::transition)
          .getOrElse { logInvalidTransition(t) }

      handleTransition()
    }
  }

  private fun logInvalidTransition(t: StateMachine.Transition<State, Event, Command>): () -> Unit =
    { logger.error("Failed to change state {} using command {}", t.fromState, t.event) }

  private fun transition(t: StateMachine.Transition.Valid<State, Event, Command>): () -> Unit =
    { processCommand(t)  }

  private fun closeConnection() {
    writer.write("221 2.0.0 Bye\n".toByteArray())
    client.close()
  }

  private fun writeStatus(code: Int, message: String) =
    writer.write("$code ${message}\n".toByteArray())

  private fun processDataStream(dataState: State.Data) {
    writer.write("354 Start mail input; end with <CRLF>.<CRLF>\n".toByteArray())

    val processor = processorFactory.create(dataState.domain, dataState.mailFrom, dataState.rcptTo)

    logger.debug("Started data retrieval")

    val lastThreeLines = CircularQueue(EndOfDataStreamPattern.size)
    while (!lastThreeLines.toArray().contentEquals(EndOfDataStreamPattern)) {
      val line = reader.nextLine()
      lastThreeLines.push(line)

      processor.write(line)
      logger.debug("Line: $line")
    }

    logger.debug("Finished data retrieval")

    writer.write("250 2.6.0 Message Accepted\n".toByteArray())
  }

  private fun processCommand(
    validTransition: StateMachine.Transition.Valid<State, Event, Command>,
  ) {
    val (command, fromState) = Pair(validTransition.sideEffect, validTransition.fromState)

    when (command) {
      is Command.Quit -> closeConnection()
      is Command.ReceiveData -> processDataStream(fromState as State.Data)
      is Command.WriteStatus -> writeStatus(command.code, command.message)
    }
  }

  private tailrec fun transitionToNextState(): Either<Throwable, StateMachine.Transition<State, Event, Command>> {
    val errorOrResult = Either.catch {
      val errorOrParsedCommand = parseCommand(reader.nextLine())

      val processParsedCommand =
        errorOrParsedCommand
          .fold(
            { e -> { logger.error(e) { "Failed to parse command" }; stateMachine.transition(Event.OnParseError) } },
            { o -> { stateMachine.transition(o) } }
          )

      processParsedCommand()
    }

    return when (errorOrResult) {
      is Either.Left -> errorOrResult
      else -> transitionToNextState()
    }
  }

  fun run() {
    logger.debug("Running ${SmtpClientHandler::class.simpleName}")

    stateMachine.transition(Event.OnConnect)

    transitionToNextState()

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
    val errorOrResult = Either.catch {
      logger.info("Waiting for connections on port (port={})", socket.localPort)
      val client = socket.accept()
      logger.info("Received connection on port (port={})", socket.localPort)

      runSmtpClientHandler(client)
    }

    when (errorOrResult) {
      is Either.Left -> return
      else -> waitForConnections()
    }
  }

  fun run(): SmtpServer {
    thread {
      waitForConnections()
    }

    return this
  }
}
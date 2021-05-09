package io.deckers.smtpjer

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import com.tinder.StateMachine
import io.deckers.smtpjer.backends.file.FileDataProcessorFactory
import io.deckers.smtpjer.parsers.parseCommand
import io.deckers.smtpjer.state_machines.Command
import io.deckers.smtpjer.state_machines.Event
import io.deckers.smtpjer.state_machines.SmtpStateMachine
import io.deckers.smtpjer.state_machines.State
import mu.KotlinLogging
import java.io.Closeable
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

private val EndOfDataStreamPattern = arrayOf("", ".", "")

private fun logEvent(event: Event) =
  when (event) {
    is Event.OnConnect -> logger.error("Established connection")
    is Event.OnData -> logger.debug("Received DATA")
    is Event.OnEhlo -> logger.debug("Received EHLO {}", event.domain)
    is Event.OnHelo -> logger.debug("Received HELO {}", event.domain)
    is Event.OnMailFrom -> logger.debug("Received MAIL FROM {}", event.emailAddress)
    is Event.OnRcptTo -> logger.debug("Received RCPT TO {}", event.emailAddress)
    is Event.OnQuit -> logger.debug("Received QUIT")
    else -> logger.error("Received unknown event ${event::class.simpleName}")
  }

private fun logInvalidTransition(t: StateMachine.Transition<State, Event, Command>): () -> Unit =
  { logger.error("Failed to change state {} using command {}", t.fromState, t.event) }

private class SmtpClientHandler(private val client: Socket) : Closeable {
  private val reader: Scanner = Scanner(client.getInputStream())
  private val writer = client.getOutputStream()
  private val processorFactory = FileDataProcessorFactory()
  private val stateMachine = SmtpStateMachine.create {
    onTransition {
      logEvent(it.event)

      val handleSideEffects =
        Option.fromNullable(it as? StateMachine.Transition.Valid)
          .map(::processSideEffects)
          .getOrElse { logInvalidTransition(it) }

      handleSideEffects()
    }
  }

  private fun processSideEffects(t: StateMachine.Transition.Valid<State, Event, Command>): () -> Unit =
    { processCommand(t) }

  private fun closeConnection() {
    logger.debug("Closing connection to {}:{}", client.inetAddress, client.port)
    writer.write("221 2.0.0 Bye\n".toByteArray())
    client.close()
    logger.debug("Closed connection to {}:{}", client.inetAddress, client.port)
  }

  private fun writeStatus(code: Int, message: String) =
    writer.write("$code ${message}\n".toByteArray())

  private fun processDataStream(dataState: State.Data) {
    writer.write("354 Start mail input; end with <CRLF>.<CRLF>\n".toByteArray())

    val processor = processorFactory.create(dataState.domain, dataState.mailFrom, dataState.rcptTo)

    logger.debug("Starting data retrieval")

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

class SmtpServer constructor(port: Int) : Closeable {
  private val socket = ServerSocket(port)

  override fun close() {
    logger.debug("Closing ${SmtpServer::class.simpleName}")

    val message = "Closed ${SmtpServer::class.simpleName}"
    if (socket.isClosed) {
      logger.debug(message)
      return
    }

    socket.close()
    logger.debug(message)
  }

  private tailrec fun waitForConnections() {
    val errorOrResult = Either.catch {
      logger.info("Waiting for connections on port (port={})", socket.localPort)
      val client = socket.accept()
      logger.info("Received connection on port (port={})", socket.localPort)

      SmtpClientHandler(client).use(SmtpClientHandler::run)
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
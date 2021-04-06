import arrow.core.Either
import mu.KotlinLogging
import java.io.Closeable
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import kotlin.concurrent.thread

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
const val COMMAND_RCPT_TO = "RCPT_TO:"
const val COMMAND_DATA = "DATA"

const val STATE_EHLO = "STATE_EHLO"
const val STATE_MAILERS = "STATE_MAILERS"
const val STATE_MAIL_TO = "STATE_MAIL_TO"
const val STATE_MAIL_FROM = "STATE_MAIL_FROM"
const val STATE_DATA = "STATE_DATA"

val stateTable = mapOf(
  STATE_EHLO to mapOf(COMMAND_EHLO to STATE_MAILERS),
  STATE_MAILERS to mapOf(
    COMMAND_MAIL_FROM to STATE_MAIL_TO,
    COMMAND_RCPT_TO to STATE_MAIL_FROM
  ),
  STATE_MAIL_TO to mapOf(COMMAND_RCPT_TO to STATE_DATA),
  STATE_MAIL_FROM to mapOf(COMMAND_MAIL_FROM to STATE_DATA),
  STATE_DATA to mapOf(COMMAND_DATA to STATE_EHLO),
)

fun <T> List<T>.destructure() = Pair(component1(), drop(1))

fun parseEhlo(line: String): Either<Throwable, List<String>> {
  val (command, parts) = line.split("\\s+").destructure()

  if (command.toUpperCase() != COMMAND_EHLO) {
    return Either.Left(Error("Expected line to start with $COMMAND_EHLO got $command"))
  }

  if (parts.count() != 1) {
    return Either.Left(Error("$COMMAND_EHLO takes a single parameter (parameter=domain)"))
  }

  return Either.Right(listOf(command) + parts)
}

fun parseMailFrom(line: String): Either<Throwable, List<String>> {
  val (command, parts) = line.split("\\s+").destructure()

  if (command.toUpperCase() != COMMAND_MAIL_FROM) {
    return Either.Left(Error("Expected line to start with $COMMAND_MAIL_FROM got $command"))
  }

  if (parts.count() != 1) {
    return Either.Left(Error("$COMMAND_MAIL_FROM takes a single parameter (parameter=e-mail address)"))
  }

  return Either.Right(listOf(command) + parts)
}

fun parseRcptTo(line: String): Either<Throwable, List<String>> {
  val (command, parts) = line.split("\\s+").destructure()

  if (command.toUpperCase() != COMMAND_RCPT_TO) {
    return Either.Left(Error("Expected line to start with $COMMAND_RCPT_TO got $command"))
  }

  if (parts.count() != 1) {
    return Either.Left(Error("$COMMAND_RCPT_TO takes a single parameter (parameter=e-mail address)"))
  }

  return Either.Right(listOf(command) + parts)
}

fun parseData(line: String): Either<Throwable, List<String>> = Either.Right(line.split("\\s+"))

private val parsers = mapOf(
  COMMAND_EHLO to ::parseEhlo,
  COMMAND_MAIL_FROM to ::parseMailFrom,
  COMMAND_RCPT_TO to ::parseRcptTo,
  COMMAND_DATA to ::parseData
)

private class SmtpStateMachine {
  var state = STATE_EHLO

  fun next(cmd: String, parameters: List<String>) {
    val maybeNextState = stateTable[state]?.get(cmd)

    if (maybeNextState == null) {
      return
    }

    // Do something funny

    state = maybeNextState
  }
}

fun parse(line: String): Either<Throwable, List<String>> {
  val maybeKey = parsers.keys.firstOrNull { line.startsWith(it, true) }

  val parse =
    maybeKey?.let { key -> parsers[key] } ?: { Either.Left(Error("Could not find matching command")) }

  return parse(line)
}

private class SmtpClientHandler(private val client: Socket) {
  private val reader: Scanner = Scanner(client.getInputStream())
  private val smtpStateMachine = SmtpStateMachine()

  fun run() {
    val errorOrParsedCommand = parse(reader.nextLine())

    errorOrParsedCommand.map { cmd -> smtpStateMachine.next(cmd[0], cmd.drop(1)) }
  }
}

class SmtpServer(port: Int) : Closeable {
  private val logger = KotlinLogging.logger {}
  private val server = ServerSocket(port)

  init {
    logger.debug("Server listening (port={port})", server.localPort)

    while (true) {
      val client = server.accept()
      println("Client connected: ${client.inetAddress.hostAddress}")

      thread { SmtpClientHandler(client).run() }
    }
  }

  override fun close() {
    server.close()
  }
}
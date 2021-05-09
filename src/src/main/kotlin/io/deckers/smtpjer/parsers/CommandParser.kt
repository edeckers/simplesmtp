package io.deckers.smtpjer.parsers

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import io.deckers.smtpjer.state_machines.*

private const val CommandData = "DATA"
private const val CommandEhlo = "EHLO"
private const val CommandHelo = "HELO"
private const val CommandMailFrom = "MAIL FROM"
private const val CommandRcptTo = "RCPT TO"
private const val CommandQuit = "QUIT"

private fun testCommand(actual: String, expected: String) = actual.toUpperCase() == expected

private fun testNumParams(actual: List<String>, expected: Int) = actual.size == expected

private fun unexpectedCommand(actual: String, expected: String) =
  Either.Left(Error("Expected line to start with $expected got $actual"))

private fun unexpectedNumParams(command: String, actualCount: Int, expected: List<String>): Either.Left<Error> {
  val message =
    if (expected.size == 1)
      "$command takes a single parameter (parameter=${expected.first()}}), received $actualCount"
    else
      "$command takes ${expected.size} parameters (parameters=${expected.joinToString(", ")}), received $actualCount"

  return Either.Left(Error(message))
}

private fun stripCommand(line: String) = line.trim().replace("\\s+".toRegex(), " ")

private fun readCommand(line: String, separator: Char) =
  with(stripCommand(line)) {
    val firstOccurrence = indexOfFirst { c -> c == separator }
    val index = if (firstOccurrence > -1) firstOccurrence else length

    val command = substring(0, index)

    val params =
      Option(substring(kotlin.math.min(index + 1, length)))
        .filter(String::isNotEmpty)
        .map { it.trim().split("\\s+".toRegex()) }
        .getOrElse(::emptyList)

    Pair(command, params)
  }

private fun parseEhlo(line: String): Either<Throwable, Event> =
  readCommand(line, ' ').let { (command, params) ->
    when {
      !testCommand(command, CommandEhlo) -> unexpectedCommand(command, CommandEhlo)
      !testNumParams(params, 1) -> unexpectedNumParams(command, params.size, listOf("domain"))
      else -> DomainName.parse(params[0]).map(Event::OnEhlo)
    }
  }

private fun parseHelo(line: String): Either<Throwable, Event> =
  readCommand(line, ' ').let { (command, params) ->
    when {
      !testCommand(command, CommandHelo) -> unexpectedCommand(command, CommandHelo)
      !testNumParams(params, 1) -> unexpectedNumParams(command, params.size, listOf("domain"))
      else -> DomainName.parse(params[0]).map(Event::OnHelo)
    }
  }

private fun parseMailFrom(line: String): Either<Throwable, Event> =
  readCommand(line, ':').let { (command, params) ->
    when {
      !testCommand(command, CommandMailFrom) -> unexpectedCommand(command, CommandMailFrom)
      !testNumParams(params, 1) -> unexpectedNumParams(command, params.size, listOf("e-mail address"))
      else -> EmailAddress.parse(params[0]).map(Event::OnMailFrom)
    }
  }

private fun parseRcptTo(line: String): Either<Throwable, Event> =
  readCommand(line, ':').let { (command, params) ->
    when {
      !testCommand(command, CommandRcptTo) -> unexpectedCommand(command, CommandRcptTo)
      !testNumParams(params, 1) -> unexpectedNumParams(command, params.size, listOf("e-mail address"))
      else -> EmailAddress.parse(params[0]).map(Event::OnRcptTo)
    }
  }

private fun parseData(line: String): Either<Throwable, Event> =
  readCommand(line, ' ').let { (command, params) ->
    when {
      !testCommand(command, CommandData) -> unexpectedCommand(command, CommandData)
      !testNumParams(params, 0) -> unexpectedNumParams(command, params.size, emptyList())
      else -> Either.Right(Event.OnData)
    }
  }

private fun parseQuit(line: String): Either<Throwable, Event> =
  readCommand(line, ' ').let { (command, params) ->
    when {
      !testCommand(command, CommandQuit) -> unexpectedCommand(command, CommandQuit)
      !testNumParams(params, 0) -> unexpectedNumParams(command, params.size, emptyList())
      else -> Either.Right(Event.OnQuit)
    }
  }

private val parsers = mapOf(
  CommandData to ::parseData,
  CommandEhlo to ::parseEhlo,
  CommandHelo to ::parseHelo,
  CommandMailFrom to ::parseMailFrom,
  CommandQuit to ::parseQuit,
  CommandRcptTo to ::parseRcptTo,
)

fun parseCommand(line: String): Either<Throwable, Event> {
  val trimmedLine = stripCommand(line)
  val maybeKey = parsers.keys.firstOrNull { trimmedLine.startsWith(it, true) }

  val parseLine =
    maybeKey?.let { key -> parsers[key] }
      ?: { Either.Left(Error("Could not find matching command for '$trimmedLine'")) }

  return parseLine(trimmedLine)
}
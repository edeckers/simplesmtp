package io.deckers.smtpjer.parsers

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import io.deckers.smtpjer.state_machine.*

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

private fun unexpectedNumParams(command: String, number: Int, expected: List<String>): Either.Left<Error> {
  val message =
    if (expected.size == 1)
      "$command takes a single parameter (parameter=${expected.first()}})"
    else
      "$command takes ${expected.size} parameters (parameters=${expected.joinToString(", ")})"

  return Either.Left(Error(message))
}

private fun stripCommand(line: String) = line.replace("\\s+".toRegex(), " ")

private fun readCommand(line: String, separator: Char) =
  with(stripCommand(line)) {
    val firstOccurrence = indexOfFirst { c -> c == separator }
    val index = if (firstOccurrence > -1) firstOccurrence else length

    val command = substring(0, index)

    val params =
      Option(substring(kotlin.math.min(index + 1, length)))
        .filter(String::isNotEmpty)
        .map { it.trim().split("\\s+".toRegex()) }
        .getOrElse { emptyList() }

    Pair(command, params)
  }

private fun parseEhlo(line: String): Either<Throwable, Event> {
  val (command, params) = readCommand(line, ' ')

  if (!testCommand(command, CommandEhlo)) {
    return unexpectedCommand(command, CommandEhlo)
  }

  if (!testNumParams(params, 1)) {
    return unexpectedNumParams(command, params.size, listOf("domain"))
  }

  return DomainName.parse(params[0]).map(Event::OnEhlo)
}

private fun parseHelo(line: String): Either<Throwable, Event> {
  val (command, params) = readCommand(line, ' ')

  if (!testCommand(command, CommandHelo)) {
    return unexpectedCommand(command, CommandHelo)
  }

  if (!testNumParams(params, 1)) {
    return unexpectedNumParams(command, params.size, listOf("domain"))
  }

  return DomainName.parse(params[0]).map(Event::OnHelo)
}

private fun parseMailFrom(line: String): Either<Throwable, Event> {
  val (command, params) = readCommand(line, ':')

  if (!testCommand(command, CommandMailFrom)) {
    return unexpectedCommand(command, CommandMailFrom)
  }

  if (!testNumParams(params, 1)) {
    return unexpectedNumParams(command, params.size, listOf("e-mail address"))
  }

  return EmailAddress.parse(params[0]).map(Event::OnMailFrom)
}

private fun parseRcptTo(line: String): Either<Throwable, Event> {
  val (command, params) = readCommand(line, ':')

  if (!testCommand(command, CommandRcptTo)) {
    return unexpectedCommand(command, CommandRcptTo)
  }

  if (!testNumParams(params, 1)) {
    return unexpectedNumParams(command, params.size, listOf("e-mail address"))
  }

  return EmailAddress.parse(params[0]).map(Event::OnRcptTo)
}

private fun parseData(line: String): Either<Throwable, Event> {
  val (command, params) = readCommand(line, ' ')

  if (!testCommand(command, CommandData)) {
    return unexpectedCommand(command, CommandData)
  }

  if (!testNumParams(params, 0)) {
    return unexpectedNumParams(command, params.size, emptyList())
  }

  return Either.Right(Event.OnData)
}

private fun parseQuit(line: String): Either<Throwable, Event> {
  val (command, params) = readCommand(line, ' ')

  if (!testCommand(command, CommandQuit)) {
    return unexpectedCommand(command, CommandQuit)
  }

  if (!testNumParams(params, 0)) {
    return unexpectedNumParams(command, params.size, emptyList())
  }

  return Either.Right(Event.OnQuit)
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
  val trimmedLine = line.trim()
  val maybeKey = parsers.keys.firstOrNull { trimmedLine.startsWith(it, true) }

  val parseLine =
    maybeKey?.let { key -> parsers[key] }
      ?: { Either.Left(Error("Could not find matching command for '$trimmedLine'")) }

  return parseLine(trimmedLine)
}
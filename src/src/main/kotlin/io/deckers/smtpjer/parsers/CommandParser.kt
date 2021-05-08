package io.deckers.smtpjer.parsers

import arrow.core.Either
import io.deckers.smtpjer.*
import io.deckers.smtpjer.state_machine.*

private const val CommandData = "DATA"
private const val CommandEhlo = "EHLO"
private const val CommandHelo = "HELO"
private const val CommandMailFrom = "MAIL FROM"
private const val CommandRcptTo = "RCPT TO"
private const val CommandQuit = "QUIT"

private fun testCommand(actual: String, expected: String) = actual.toUpperCase() == expected
private fun testNumParams(actual: List<String>, expected: Int) = actual.size == expected

private fun parseEhlo(line: String): Either<Throwable, Event> {
  val strippedLine = stripCommand(line)
  val (command, params) = strippedLine.split("\\s+".toRegex()).destructure()

  if (!testCommand(command, CommandEhlo)) {
    return Either.Left(Error("Expected line to start with $CommandEhlo got $command"))
  }

  if (!testNumParams(params, 1)) {
    return Either.Left(Error("$CommandEhlo takes a single parameter (parameter=domain)"))
  }

  return DomainName.parse(params[0]).map(Event::OnEhlo)
}

private fun parseHelo(line: String): Either<Throwable, Event> {
  val strippedLine = stripCommand(line)
  val (command, params) = strippedLine.split("\\s+".toRegex()).destructure()

  if (!testCommand(command, CommandHelo)) {
    return Either.Left(Error("Expected line to start with $CommandHelo got $command"))
  }

  if (!testNumParams(params, 1)) {
    return Either.Left(Error("$CommandHelo takes a single parameter (parameter=domain)"))
  }

  return DomainName.parse(params[0]).map(Event::OnHelo)
}

private fun parseMailFrom(line: String): Either<Throwable, Event> {
  val strippedLine = stripCommand(line)
  val (command, rest) = strippedLine.split(":").map { it.trim() }.destructure()

  val params = if (rest.isNotEmpty()) rest[0].trim().split("\\s+".toRegex()) else emptyList()

  if (!testCommand(command, CommandMailFrom)) {
    return Either.Left(Error("Expected line to start with $CommandMailFrom got $command"))
  }

  if (!testNumParams(params, 1)) {
    return Either.Left(Error("$CommandMailFrom takes a single parameter (parameter=e-mail address)"))
  }

  return EmailAddress.parse(params[0]).map(Event::OnMailFrom)
}

private fun parseRcptTo(line: String): Either<Throwable, Event> {
  val strippedLine = stripCommand(line)
  val (command, params) = strippedLine.split(":").map { it.trim() }.destructure()

  if (!testCommand(command, CommandRcptTo)) {
    return Either.Left(Error("Expected line to start with $CommandRcptTo got $command"))
  }

  if (!testNumParams(params, 1)) {
    return Either.Left(Error("$CommandRcptTo takes a single parameter (parameter=e-mail address)"))
  }

  return EmailAddress.parse(params[0]).map(Event::OnRcptTo)
}

private fun parseData(line: String): Either<Throwable, Event> {
  val strippedLine = stripCommand(line)
  val (command, params) = strippedLine.split("\\s+".toRegex()).destructure()

  if (!testCommand(command, CommandData)) {
    return Either.Left(Error("Expected line to start with $CommandData got $command"))
  }

  if (!testNumParams(params, 0)) {
    return Either.Left(Error("$CommandData takes zero parameters"))
  }

  return Either.Right(Event.OnData)
}

private fun parseQuit(line: String): Either<Throwable, Event> {
  val strippedLine = stripCommand(line)
  val (command, params) = strippedLine.split("\\s+".toRegex()).destructure()

  if (command.toUpperCase() != CommandQuit) {
    return Either.Left(Error("Expected line to start with $CommandQuit got $strippedLine"))
  }

  if (!testNumParams(params, 0)) {
    return Either.Left(Error("$CommandData takes zero parameters"))
  }

  return Either.Right(Event.OnQuit)
}

private fun stripCommand(line: String) = line.replace("\\s+".toRegex(), " ")

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
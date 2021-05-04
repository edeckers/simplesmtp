package io.deckers.smtpjer.parsers

import arrow.core.Either
import io.deckers.smtpjer.*
import io.deckers.smtpjer.state_machine.Event

private fun parseEhlo(line: String): Either<Throwable, Event> {
  val strippedLine = stripCommand(line)
  val (command, parts) = strippedLine.split("\\s+".toRegex()).destructure()

  if (command.toUpperCase() != CommandEhlo) {
    return Either.Left(Error("Expected line to start with $CommandEhlo got $command"))
  }

  if (parts.count() != 1) {
    return Either.Left(Error("$CommandEhlo takes a single parameter (parameter=domain)"))
  }

  return Either.Right(Event.OnEhlo(parts[0]))
}

private fun parseHelo(line: String): Either<Throwable, Event> {
  val strippedLine = stripCommand(line)
  val (command, parts) = strippedLine.split("\\s+".toRegex()).destructure()

  if (command.toUpperCase() != CommandHelo) {
    return Either.Left(Error("Expected line to start with $CommandHelo got $command"))
  }

  if (parts.count() != 1) {
    return Either.Left(Error("$CommandHelo takes a single parameter (parameter=domain)"))
  }

  return Either.Right(Event.OnHelo(parts[0]))
}

private fun parseMailFrom(line: String): Either<Throwable, Event> {
  val strippedLine = stripCommand(line)
  val (command, rest) = strippedLine.split(":").map { it.trim() }.destructure()

  val parts = if (rest.isNotEmpty()) rest[0].trim().split("\\s+".toRegex()) else emptyList()

  if (command.toUpperCase() != CommandMailFrom) {
    return Either.Left(Error("Expected line to start with $CommandMailFrom got $command"))
  }

  if (parts.count() != 1) {
    return Either.Left(Error("$CommandMailFrom takes a single parameter (parameter=e-mail address)"))
  }

  return EmailAddress.parse(parts[0]).map(Event::OnMailFrom)
}

private fun parseRcptTo(line: String): Either<Throwable, Event> {
  val strippedLine = stripCommand(line)
  val (command, parts) = strippedLine.split(":").map { it.trim() }.destructure()

  if (command.toUpperCase() != CommandRcptTo) {
    return Either.Left(Error("Expected line to start with $CommandRcptTo got $command"))
  }

  if (parts.count() != 1) {
    return Either.Left(Error("$CommandRcptTo takes a single parameter (parameter=e-mail address)"))
  }

  return EmailAddress.parse(parts[0]).map(Event::OnRcptTo)
}

private fun parseData(line: String): Either<Throwable, Event> {
  val strippedLine = stripCommand(line)
  if (strippedLine.toUpperCase() != CommandData) {
    return Either.Left(Error("Expected line to start with $CommandData got $strippedLine"))
  }

  return Either.Right(Event.OnData)
}

private fun parseQuit(line: String): Either<Throwable, Event> {
  val strippedLine = stripCommand(line)
  if (strippedLine.toUpperCase() != CommandQuit) {
    return Either.Left(Error("Expected line to start with $CommandQuit got $strippedLine"))
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

fun parse(line: String): Either<Throwable, Event> {
  val trimmedLine = line.trim()
  val maybeKey = parsers.keys.firstOrNull { trimmedLine.startsWith(it, true) }

  val parseLine =
    maybeKey?.let { key -> parsers[key] }
      ?: { Either.Left(Error("Could not find matching command for '$trimmedLine'")) }

  return parseLine(trimmedLine)
}
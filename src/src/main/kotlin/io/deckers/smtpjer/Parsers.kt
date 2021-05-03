package io.deckers.smtpjer

import arrow.core.Either

private fun parseEhlo(line: String): Either<Throwable, Event> {
  val strippedLine = line.replace("\\s+".toRegex(), " ")
  val (command, parts) = strippedLine.split("\\s+".toRegex()).destructure()

  if (command.toUpperCase() != COMMAND_EHLO) {
    return Either.Left(Error("Expected line to start with $COMMAND_EHLO got $command"))
  }

  if (parts.count() != 1) {
    return Either.Left(Error("$COMMAND_EHLO takes a single parameter (parameter=domain)"))
  }

  return Either.Right(Event.OnEhlo(parts[0]))
}

private fun parseHelo(line: String): Either<Throwable, Event> {
  val (command, parts) = line.split("\\s+".toRegex()).destructure()

  if (command.toUpperCase() != COMMAND_HELO) {
    return Either.Left(Error("Expected line to start with $COMMAND_HELO got $command"))
  }

  if (parts.count() != 1) {
    return Either.Left(Error("$COMMAND_HELO takes a single parameter (parameter=domain)"))
  }

  return Either.Right(Event.OnHelo(parts[0]))
}

private fun parseMailFrom(line: String): Either<Throwable, Event> {
  val (command, parts) = line.split(":").map { it.trim() }.destructure()

  if (command.toUpperCase() != COMMAND_MAIL_FROM) {
    return Either.Left(Error("Expected line to start with $COMMAND_MAIL_FROM got $command"))
  }

  if (parts.count() != 1) {
    return Either.Left(Error("$COMMAND_MAIL_FROM takes a single parameter (parameter=e-mail address)"))
  }

  return EmailAddress.parse(parts[0]).map(Event::OnMailFrom)
}

private fun parseRcptTo(line: String): Either<Throwable, Event> {
  val (command, parts) = line.split(":").map { it.trim() }.destructure()

  if (command.toUpperCase() != COMMAND_RCPT_TO) {
    return Either.Left(Error("Expected line to start with $COMMAND_RCPT_TO got $command"))
  }

  if (parts.count() != 1) {
    return Either.Left(Error("$COMMAND_RCPT_TO takes a single parameter (parameter=e-mail address)"))
  }

  return EmailAddress.parse(parts[0]).map(Event::OnRcptTo)
}

private fun parseData(line: String): Either<Throwable, Event> {
  if (line.trim().toUpperCase() != COMMAND_DATA) {
    return Either.Left(Error("Expected line to start with $COMMAND_DATA got $line"))
  }

  return Either.Right(Event.OnData)
}

private fun parseQuit(line: String): Either<Throwable, Event> {
  if (line.trim().toUpperCase() != COMMAND_QUIT) {
    return Either.Left(Error("Expected line to start with $COMMAND_QUIT got $line"))
  }

  return Either.Right(Event.OnQuit)
}

private val parsers = mapOf(
  COMMAND_DATA to ::parseData,
  COMMAND_EHLO to ::parseEhlo,
  COMMAND_HELO to ::parseHelo,
  COMMAND_MAIL_FROM to ::parseMailFrom,
  COMMAND_QUIT to ::parseQuit,
  COMMAND_RCPT_TO to ::parseRcptTo,
)

fun parse(line: String): Either<Throwable, Event> {
  val trimmedLine = line.trim()
  val maybeKey = parsers.keys.firstOrNull { trimmedLine.startsWith(it, true) }

  val parseLine =
    maybeKey?.let { key -> parsers[key] } ?: { Either.Left(Error("Could not find matching command for '$trimmedLine'")) }

  return parseLine(trimmedLine)
}
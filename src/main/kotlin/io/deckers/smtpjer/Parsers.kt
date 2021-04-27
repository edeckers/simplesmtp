package io.deckers.smtpjer

import arrow.core.Either


private fun parseEhlo(line: String): Either<Throwable, List<String>> {
  val xs = line.split("\\s+".toRegex())
  val (command, parts) = xs.destructure()

  if (command.toUpperCase() != COMMAND_EHLO) {
    return Either.Left(Error("Expected line to start with $COMMAND_EHLO got $command"))
  }

  if (parts.count() != 1) {
    return Either.Left(Error("$COMMAND_EHLO takes a single parameter (parameter=domain)"))
  }

  return Either.Right(listOf(command.toUpperCase()) + parts)
}

private fun parseMailFrom(line: String): Either<Throwable, List<String>> {
  val (command, parts) = listOf(
    line.substring(0, COMMAND_MAIL_FROM.length),
    line.substring(COMMAND_MAIL_FROM.length).trim()
  ).destructure()

  if (command.toUpperCase() != COMMAND_MAIL_FROM) {
    return Either.Left(Error("Expected line to start with $COMMAND_MAIL_FROM got $command"))
  }

  if (parts.count() != 1) {
    return Either.Left(Error("$COMMAND_MAIL_FROM takes a single parameter (parameter=e-mail address)"))
  }

  return Either.Right(listOf(command.toUpperCase()) + parts)
}

private fun parseRcptTo(line: String): Either<Throwable, List<String>> {
  val (command, parts) = listOf(
    line.substring(0, COMMAND_RCPT_TO.length),
    line.substring(COMMAND_RCPT_TO.length).trim()
  ).destructure()

  if (command.toUpperCase() != COMMAND_RCPT_TO) {
    return Either.Left(Error("Expected line to start with $COMMAND_RCPT_TO got $command"))
  }

  if (parts.count() != 1) {
    return Either.Left(Error("$COMMAND_RCPT_TO takes a single parameter (parameter=e-mail address)"))
  }

  return Either.Right(listOf(command.toUpperCase()) + parts)
}

private fun parseData(line: String): Either<Throwable, List<String>> {
  if (line.trim().toUpperCase() != COMMAND_DATA) {
    return Either.Left(Error("Expected line to start with $COMMAND_DATA got $line"))
  }

  return Either.Right(listOf(COMMAND_DATA))
}

private val parsers = mapOf(
  COMMAND_EHLO to ::parseEhlo,
  COMMAND_MAIL_FROM to ::parseMailFrom,
  COMMAND_RCPT_TO to ::parseRcptTo,
  COMMAND_DATA to ::parseData
)

fun parse(line: String): Either<Throwable, List<String>> {
  val maybeKey = parsers.keys.firstOrNull { line.startsWith(it, true) }

  val parseLine =
    maybeKey?.let { key -> parsers[key] } ?: { Either.Left(Error("Could not find matching command")) }

  return parseLine(line)
}
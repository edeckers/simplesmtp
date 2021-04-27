package io.deckers.smtpjer

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal class SmtpStateMachine {
  private var state = STATE_EHLO

  fun next(cmd: String, parameters: List<String>) {
    logger.info("Received command (cmd={})", cmd)
    val maybeNextState = stateTable[state]?.get(cmd)

    if (maybeNextState == null) {
      logger.error("Failed to change state {} using command {}", state, cmd)
      return
    }

    // Do something funny
    logger.debug("Change from {} to {}", state, maybeNextState)

    state = maybeNextState
  }
}
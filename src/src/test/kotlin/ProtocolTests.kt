/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class ProtocolTests {
  @Test
  @Tag(EndToEnd)
  fun assert_ehlo_returns_500() = withClient {
    // Arrange
    val welcomeResponse = read()

    // Act
    write("EHLO domain.com")
    val ehloResponse = read()

    // Assert
    assertResponse(220, welcomeResponse)
    assertResponse(500, ehloResponse)
  }

  @Test
  @Tag(EndToEnd)
  fun assert_helo_returns_250() = withClient {
    // Arrange
    val welcomeResponse = read()

    // Act
    write("HELO domain.com")
    val heloResponse = read()

    // Assert
    assertResponse(220, welcomeResponse)
    assertResponse(250, heloResponse)
  }

  @Test
  @Tag(EndToEnd)
  fun assert_mail_from_returns_250() = withClient {
    // Arrange
    discard()

    discard { helo("domain.com") }

    // Act
    write("MAIL FROM: mailbox@domain.com")
    val mailFromResponse = read()

    // Assert
    assertResponse(250, mailFromResponse)
  }

  @Test
  @Tag(EndToEnd)
  fun assert_rcpt_to_returns_250() = withClient {
    // Arrange
    discard()

    discard { helo("domain.com") }

    discard { mail("mailbox@domain.com") }

    // Act
    write("RCPT TO: mailbox@domain.com")
    val rcptToResponse = read()

    // Assert
    assertResponse(250, rcptToResponse)
  }

  @Test
  @Tag(EndToEnd)
  fun assert_data_returns_354() = withClient {
    // Arrange
    discard()

    discard { helo("domain.com") }

    discard { mail("mailbox@domain.com") }

    discard { toRecipient("mailbox@domain.com") }

    // Act
    write("DATA")
    val dataToResponse = read()

    // Assert
    assertResponse(354, dataToResponse)
  }

  @Test
  @Tag(EndToEnd)
  fun assert_data_finish_returns_250() = withClient {
    // Arrange
    discard()

    discard { helo("domain.com") }

    discard { mail("mailbox@domain.com") }

    discard { toRecipient("mailbox@domain.com") }

    // Act
    discard { data() }

    write("this is some irrelevant data that")
    write("spans multiple lines, to demonstrate")
    write("that the connection is kept until the")
    write("kill sequence is received.")

    write("")
    write(".")
    write("")

    val dataEndResponse = read()

    // Assert
    assertResponse(250, dataEndResponse)
  }

  @Test
  @Tag(EndToEnd)
  fun assert_data_obscured_kill_sequence_return_250() = withClient {
    // Arrange
    runToData()

    // Act
    write("")
    write(".")
    write(".")
    write("")
    write("")
    write(".")
    write("a")
    write("")
    write(".")
    write("")

    val dataEndResponse = read()

    // Assert
    assertResponse(250, dataEndResponse)
  }
}
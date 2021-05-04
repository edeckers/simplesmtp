import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

class ProtocolTests {

  @Test
  fun assert_ehlo_returns_500() = runContext { _, client ->

    val reader = Scanner(client.getInputStream())
    val welcomeResponse = reader.nextLine()

    val writer = client.getOutputStream()

    writer.write("EHLO domain.com\n".toByteArray())
    val ehloResponse = reader.nextLine()

    assertEquals("220", welcomeResponse.substring(0, 3))
    assertEquals("500", ehloResponse.substring(0, 3))
  }

  @Test
  fun assert_helo_returns_250() = runContext { _, client ->

    val reader = Scanner(client.getInputStream())
    val welcomeResponse = reader.nextLine()

    val writer = client.getOutputStream()

    writer.write("HELO domain.com\n".toByteArray())
    val heloResponse = reader.nextLine()

    assertEquals("220", welcomeResponse.substring(0, 3))
    assertEquals("250", heloResponse.substring(0, 3))
  }

  @Test
  fun assert_mail_from_returns_250() = runContext { _, client ->

    val reader = Scanner(client.getInputStream())
    reader.nextLine()

    val writer = client.getOutputStream()

    writer.write("HELO domain.com\n".toByteArray())
    reader.nextLine()

    writer.write("MAIL FROM: mailbox@domain.com\n".toByteArray())
    val mailFromResponse = reader.nextLine()

    assertEquals("250", mailFromResponse.substring(0, 3))
  }

  @Test
  fun assert_rcpt_to_returns_250() = runContext { _, client ->

    val reader = Scanner(client.getInputStream())
    reader.nextLine()

    val writer = client.getOutputStream()

    writer.write("HELO domain.com\n".toByteArray())
    reader.nextLine()

    writer.write("MAIL FROM: mailbox@domain.com\n".toByteArray())
    reader.nextLine()

    writer.write("RCPT TO: mailbox@domain.com\n".toByteArray())
    val rcptToResponse = reader.nextLine()

    assertEquals("250", rcptToResponse.substring(0, 3))
  }

  @Test
  fun assert_data_returns_354() = runContext { _, client ->
    val reader = Scanner(client.getInputStream())
    reader.nextLine()

    val writer = client.getOutputStream()

    writer.write("HELO domain.com\n".toByteArray())
    reader.nextLine()

    writer.write("MAIL FROM: mailbox@domain.com\n".toByteArray())
    reader.nextLine()

    writer.write("RCPT TO: mailbox@domain.com\n".toByteArray())
    reader.nextLine()

    writer.write("DATA\n".toByteArray())
    val dataToResponse = reader.nextLine()

    assertEquals("354", dataToResponse.substring(0, 3))
  }

  @Test
  fun assert_data_finish_returns_250() = runContext { _, client ->
    val reader = Scanner(client.getInputStream())
    reader.nextLine()

    val writer = client.getOutputStream()

    writer.write("HELO domain.com\n".toByteArray())
    reader.nextLine()

    writer.write("MAIL FROM: mailbox@domain.com\n".toByteArray())
    reader.nextLine()

    writer.write("RCPT TO: mailbox@domain.com\n".toByteArray())
    reader.nextLine()

    writer.write("DATA\n".toByteArray())
    reader.nextLine()

    writer.write("\n".toByteArray())

    writer.write(".\n".toByteArray())

    writer.write("\n".toByteArray())
    val dataEndResponse = reader.nextLine()

    assertEquals("250", dataEndResponse.substring(0, 3))
  }
}
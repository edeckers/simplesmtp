/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import arrow.core.Either
import io.deckers.smtpjer.services.SmtpServer
import io.deckers.smtpjer.state_machines.Event
import java.io.Closeable
import java.net.Socket
import java.util.*
import kotlin.test.assertEquals

private const val ServerPort = 9999

const val ValidEhloCommand = "EHLO infi.nl"
const val ValidHeloCommand = "HELO infi.nl"
const val ValidMailFromCommand = "MAIL FROM: mailbox@domain.com"
const val ValidRcptToCommand = "RCPT TO: mailbox@domain.com"
const val ValidDataCommand = "DATA"
const val ValidQuitCommand = "QUIT"  // region E-mail address

fun dataProcessorFactory() = DummyProcessorFactory()

fun server() = SmtpServer(ServerPort, dataProcessorFactory()).run()
fun client() = Socket("127.0.0.1", ServerPort)

inline fun <reified T> toEvent(e: Event): Either<Throwable, T> =
  if (e is T) Either.Right(e) else Either.Left(Error(""))

open class ClientDSL(private val server: SmtpServer, private val client: Socket) : Closeable {
  private val reader = Scanner(client.getInputStream())
  private val writer = client.getOutputStream()

  fun write(line: String) = writer.write("$line\n".toByteArray())
  fun read() = reader.nextLine()
  fun discard() {
    reader.nextLine()
  }

  override fun close() {
    reader.close()
    writer.close()
    client.close()
    server.close()
  }
}

open class TestSmtpClientDSL(server: SmtpServer, client: Socket) : ClientDSL(server, client) {
  fun data() = write("DATA")
  fun ehlo(domain: String) = write("EHLO $domain")
  fun helo(domain: String) = write("HELO $domain")
  fun mail(from: String) = write("MAIL FROM: $from")
  fun toRecipient(to: String) = write("RCPT TO: $to")

  fun discard(fn: () -> Unit) = run {
    fn()
    discard()
  }

  fun runToData() = run {
    discard()
    discard { helo("domain.com") }
    discard { mail("mailbox@domain.com") }
    discard { toRecipient("yes@no.com") }
    discard { data() }
  }
}

class TestClientDSL(server: SmtpServer, client: Socket) : TestSmtpClientDSL(server, client) {
  fun assertResponse(code: Int, message: String) = assertEquals(code.toString(10), message.substring(0, 3))
}

fun withClient(dsl: TestClientDSL.() -> Unit) {
  val client = TestClientDSL(server(), client()).apply(dsl)

  client.close()
}

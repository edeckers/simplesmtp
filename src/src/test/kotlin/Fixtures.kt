import io.deckers.smtpjer.SmtpServer
import java.io.Closeable
import java.net.Socket
import java.util.*
import kotlin.test.assertEquals

private const val ServerPort = 9999

fun server() = SmtpServer(ServerPort).run()
fun client() = Socket("127.0.0.1", ServerPort)

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

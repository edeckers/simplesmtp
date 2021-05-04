import io.deckers.smtpjer.SmtpServer
import java.net.Socket

private const val ServerPort = 9999

fun server() = SmtpServer(ServerPort)
fun client() = Socket("127.0.0.1", ServerPort)

fun runContext(fn: (server: SmtpServer, client: Socket) -> Unit) =
  server().use { server ->
    client().use { client ->
      fn(server, client)
    }
  }
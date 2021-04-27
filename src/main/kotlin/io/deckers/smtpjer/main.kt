package io.deckers.smtpjer

fun main(args: Array<String>) {
    val s = SmtpServer(9999)

    print("Press any key to quit.")
    readLine()
    s.close()
}
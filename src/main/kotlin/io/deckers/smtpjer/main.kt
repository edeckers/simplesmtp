package io.deckers.smtpjer

fun main(args: Array<String>) {
    val s = SmtpServer(9999)

    readLine()
    s.close()
}
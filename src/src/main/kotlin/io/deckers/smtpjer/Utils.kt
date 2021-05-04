package io.deckers.smtpjer

import java.io.InputStream
import java.io.OutputStream
import java.util.stream.Stream

fun <T> List<T>.destructure() = Pair(component1(), drop(1))

fun testLine(line: String, pattern: String) = line == pattern

fun testLineFeed(line: String) = line == "\\n"
fun testDot(line: String) = line == "."

fun testLines(lines: Array<String>) {
  testLineFeed(lines[0])
  testDot(lines[1])
  testLineFeed(lines[2])
}

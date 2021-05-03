package io.deckers.smtpjer

fun <T> List<T>.destructure() = Pair(component1(), drop(1))
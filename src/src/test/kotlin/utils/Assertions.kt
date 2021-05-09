package utils

import arrow.core.Either
import arrow.core.getOrElse
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

fun assertDomainNameMatchesRules(domainName: String?) {
  assertNotNull(domainName)
  assertTrue(domainName.isNotEmpty(), "Domain name length should be at least one character long")
  assertTrue(domainName.matches("[a-zA-Z0-9.-]*".toRegex()), "Domain name contains unexpected characters")
  assertNotSame('-', domainName[0], "Domain cannot start with hyphen")
}

fun assertEmailAddressMatchesRules(emailAddress: String?) {
  assertNotNull(emailAddress)
  assertTrue(emailAddress.length >= 3, "E-mail address length should be at least three character long")
  assertTrue(emailAddress.matches("[a-zA-Z0-9.-@]*".toRegex()), "E-mail address contains unexpected characters")
  assertTrue(emailAddress.contains('@'), "E-mail address must contain @")
}

fun <L, R> Either<L, R>.succeeds(message: String) = assertTrue(isRight(), message)
fun <L, R> Either<L, R>.fails(message: String) = assertTrue(isLeft(), message)
fun <L, R, S> Either<L, R>.matches(map: (b: R) -> S, expected: S, message: String) {
  val maybeMapped = this.map(map).orNull()

  assertNotNull(maybeMapped, message)
  assertEquals(expected, maybeMapped, message)
}
fun <L, R, S : Any, T : KClass<S>> Either<L, R>.isType(t: T) =
  assertEquals(t, map { it!!::class }.getOrElse { Unit::class }, "Expected ${t.simpleName}")


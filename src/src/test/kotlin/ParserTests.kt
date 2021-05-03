import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import io.deckers.smtpjer.Event
import io.deckers.smtpjer.parse
import org.junit.jupiter.api.Test
import kotlin.test.*

const val ValidEhloCommand = "EHLO infi.nl"

class ParserTests {
  @Test
  fun assert_ehlo_command_recognized_with_exactly_one_parameter() {
    val ehloWithoutParameter = parse("EHLO")
    val ehloWithSingleParameter = parse(ValidEhloCommand)
    val ehloWithMultipleParameters = parse("EHLO infi.nl nu.nl")

    assertTrue(ehloWithoutParameter.isLeft(), "Parsing parameterless EHLO should fail")
    assertEquals(Event.OnEhlo::class, ehloWithSingleParameter.getOrElse { }::class, "Expected OnEhlo")
    assertTrue(ehloWithMultipleParameters.isLeft(), "Parsing EHLO with more than a single parameter should fail")
  }

  @Test
  fun assert_ehlo_command_accepts_single_domain_parameter() {
    val ehloWithSingleParameter = parse(ValidEhloCommand)

    val domainName = ehloWithSingleParameter
      .flatMap { toEvent<Event.OnEhlo>(it) }
      .map { e -> e.domain }
      .orNull()

    assertNotNull(domainName, "No domain name was parsed")
    assert_domain_name_matches_rules(domainName)
  }

  @Test
  fun assert_ehlo_command_spaces_are_stripped() {
    val ehloWithSingleParameter = parse("   EHLO    in-fi.nl   ")

    val domainName = ehloWithSingleParameter
      .flatMap { toEvent<Event.OnEhlo>(it) }
      .map { e -> e.domain }
      .orNull()

    assertNotNull(domainName, "No domain name was parsed")
    assert_domain_name_matches_rules(domainName)
    assertEquals("in-fi.nl", domainName)
  }

  @Test
  fun assert_ehlo_command_case_is_ignored() {
    val ehloWithSingleParameter = parse("eHlO infi.nl")

    val errorOrEvent =
      ehloWithSingleParameter
        .flatMap { toEvent<Event.OnEhlo>(it) }

    val maybeEvent = errorOrEvent.orNull()

    val maybeDomainName =
      errorOrEvent
        .map { e -> e.domain }
        .orNull()

    assertNotNull(maybeEvent, "Event should be OnEhlo")
    assertEquals("infi.nl", maybeDomainName)
  }

  private fun assert_domain_name_matches_rules(domainName: String) {
    assertTrue(domainName.isNotEmpty(), "Domain name length should be at least one character long")
    assertTrue(domainName.matches("[a-zA-Z0-9.-]*".toRegex()), "Domain name contains unexpected characters")
    assertNotSame('-', domainName[0], "Domain cannot start with hyphen")
  }

  private inline fun <reified T> toEvent(e: Event): Either<Throwable, T> =
    if (e is T) Either.Right(e) else Either.Left(Error(""))
}
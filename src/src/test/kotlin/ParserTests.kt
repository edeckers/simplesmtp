import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import io.deckers.smtpjer.parsers.EmailAddress
import io.deckers.smtpjer.state_machine.Event
import io.deckers.smtpjer.parsers.parseCommand
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.test.*

private const val ValidEhloCommand = "EHLO infi.nl"
private const val ValidHeloCommand = "HELO infi.nl"
private const val ValidMailFromCommand = "MAIL FROM: mailbox@domain.com"
private const val ValidRcptToCommand = "RCPT TO: mailbox@domain.com"
private const val ValidDataCommand = "DATA"
private const val ValidQuitCommand = "QUIT"

private fun assertDomainnameMatchesRules(domainName: String?) {
  assertNotNull(domainName)
  assertTrue(domainName.isNotEmpty(), "Domain name length should be at least one character long")
  assertTrue(domainName.matches("[a-zA-Z0-9.-]*".toRegex()), "Domain name contains unexpected characters")
  assertNotSame('-', domainName[0], "Domain cannot start with hyphen")
}

private fun assertEmailAddressMatchesRules(emailAddress: String) {
  assertTrue(emailAddress.length >= 3, "E-mail address length should be at least three character long")
  assertTrue(emailAddress.matches("[a-zA-Z0-9.-@]*".toRegex()), "E-mail address contains unexpected characters")
  assertTrue(emailAddress.contains('@'), "E-mail address must contain @")
}

private inline fun <reified T> toEvent(e: Event): Either<Throwable, T> =
  if (e is T) Either.Right(e) else Either.Left(Error(""))

fun <L, R> succeeds(v: Either<L, R>, message: String) = assertTrue(v.isRight(), message)
fun <L, R> fails(v: Either<L, R>, message: String) = assertTrue(v.isLeft(), message)
fun <L, R, S> matches(v: Either<L, R>, map: (b: R) -> S, expected: S, message: String) {
  val maybeMapped = v.map(map).orNull()

  assertNotNull(maybeMapped, message)
  assertEquals(expected, maybeMapped, message)
}

fun <L, R, S : Any, T : KClass<S>> isType(v: Either<L, R>, t: T) =
  assertEquals(t, v.map { it!!::class }.getOrElse { Unit::class }, "Expected ${t.simpleName}")

class ParserTests {
  // region E-mail address
  @Test
  @Tag(Isolated)
  fun assert_email_address_validates_input() {
    val errorOrEmailAddress = EmailAddress.parse("mailbox@domain.com")

    succeeds(errorOrEmailAddress, "No email address was parsed")
    matches(errorOrEmailAddress, EmailAddress::mailbox, "mailbox", "Unexpected mailbox")
    matches(errorOrEmailAddress, EmailAddress::domainName, "domain.com", "Unexpected hostname")
    assertEmailAddressMatchesRules(errorOrEmailAddress.map(EmailAddress::address).getOrElse { "" })
  }
  // endregion

  // region EHLO
  @Test
  @Tag(Isolated)
  fun assert_ehlo_command_recognized_with_exactly_one_parameter() {
    val ehloWithoutParameter = parseCommand("EHLO")
    val ehloWithSingleParameter = parseCommand(ValidEhloCommand)
    val ehloWithMultipleParameters = parseCommand("EHLO infi.nl nu.nl")

    fails(ehloWithoutParameter, "Parsing parameterless EHLO should fail")
    isType(ehloWithSingleParameter, Event.OnEhlo::class)
    fails(ehloWithMultipleParameters, "Parsing EHLO with more than a single parameter should fail")
  }

  @Test
  @Tag(Isolated)
  fun assert_ehlo_command_accepts_single_domain_parameter() {
    val ehloWithSingleParameter = parseCommand(ValidEhloCommand)

    val maybeDomainName = ehloWithSingleParameter
      .flatMap { toEvent<Event.OnEhlo>(it) }
      .map(Event.OnEhlo::domain)
      .orNull()

    succeeds(ehloWithSingleParameter, "No domainName was parsed")
    assertDomainnameMatchesRules(maybeDomainName?.name)
  }

  @Test
  @Tag(Isolated)
  fun assert_ehlo_command_spaces_are_stripped() {
    val ehloWithSingleParameter = parseCommand("   EHLO    in-fi.nl   ")

    val maybeDomainName = ehloWithSingleParameter
      .flatMap { toEvent<Event.OnEhlo>(it) }
      .map(Event.OnEhlo::domain)
      .orNull()

    succeeds(ehloWithSingleParameter, "No domainName was parsed")
    assertDomainnameMatchesRules(maybeDomainName?.name)
    assertEquals("in-fi.nl", maybeDomainName?.name)
  }

  @Test
  @Tag(Isolated)
  fun assert_ehlo_command_case_is_ignored() {
    val ehloWithSingleParameter = parseCommand("eHlO infi.nl")

    val errorOrEvent =
      ehloWithSingleParameter
        .flatMap { toEvent<Event.OnEhlo>(it) }

    val maybeDomainName =
      errorOrEvent
        .map(Event.OnEhlo::domain)
        .orNull()

    isType(ehloWithSingleParameter, Event.OnEhlo::class)
    assertEquals("infi.nl", maybeDomainName?.name)
  }
// endregion

  // region HELO
  @Test
  @Tag(Isolated)
  fun assert_helo_command_recognized_with_exactly_one_parameter() {
    val heloWithoutParameter = parseCommand("HELO")
    val heloWithSingleParameter = parseCommand(ValidHeloCommand)
    val heloWithMultipleParameters = parseCommand("HELO infi.nl nu.nl")

    fails(heloWithoutParameter, "Parsing parameterless HELO should fail")
    isType(heloWithSingleParameter, Event.OnHelo::class)
    fails(heloWithMultipleParameters, "Parsing HELO with more than a single parameter should fail")
  }

  @Test
  @Tag(Isolated)
  fun assert_helo_command_accepts_single_domain_parameter() {
    val heloWithSingleParameter = parseCommand(ValidHeloCommand)

    val maybeDomainName = heloWithSingleParameter
      .flatMap { toEvent<Event.OnHelo>(it) }
      .map(Event.OnHelo::domain)
      .orNull()

    succeeds(heloWithSingleParameter, "No domainName was parsed")
    assertDomainnameMatchesRules(maybeDomainName?.name)
  }

  @Test
  @Tag(Isolated)
  fun assert_helo_command_spaces_are_stripped() {
    val heloWithSingleParameter = parseCommand("   HELO    in-fi.nl   ")

    val maybeDomainName = heloWithSingleParameter
      .flatMap { toEvent<Event.OnHelo>(it) }
      .map(Event.OnHelo::domain)
      .orNull()

    succeeds(heloWithSingleParameter, "No domainName was parsed")
    assertDomainnameMatchesRules(maybeDomainName?.name)
    assertEquals("in-fi.nl", maybeDomainName?.name)
  }

  @Test
  @Tag(Isolated)
  fun assert_helo_command_case_is_ignored() {
    val heloWithSingleParameter = parseCommand("helO infi.nl")

    val errorOrEvent =
      heloWithSingleParameter
        .flatMap { toEvent<Event.OnHelo>(it) }

    val maybeDomainName =
      errorOrEvent
        .map(Event.OnHelo::domain)
        .orNull()

    isType(heloWithSingleParameter, Event.OnHelo::class)
    assertEquals("infi.nl", maybeDomainName?.name)
  }
  // endregion

  // region MAIL FROM
  @Test
  @Tag(Isolated)
  fun assert_mail_from_command_recognized_with_exactly_one_parameter() {
    val mailFromWithoutParameter = parseCommand("MAIL FROM")
    val mailFromWithSingleParameter = parseCommand(ValidMailFromCommand)
    val mailFromWithMultipleParameters = parseCommand("MAIL FROM: yes@no.com some@thing.com")

    fails(mailFromWithoutParameter, "Parsing parameterless MAIL FROM should fail")
    assertEquals(Event.OnMailFrom::class, mailFromWithSingleParameter.getOrElse { }::class, "Expected OnMailFrom")
    fails(
      mailFromWithMultipleParameters,
      "Parsing MAIL FROM with more than a single parameter should fail"
    )
  }

  @Test
  @Tag(Isolated)
  fun assert_mail_from_command_accepts_single_email_address_parameter() {
    val mailFromWithSingleParameter = parseCommand(ValidMailFromCommand)

    val maybeEmailAddress =
      mailFromWithSingleParameter
        .flatMap { toEvent<Event.OnMailFrom>(it) }
        .map(Event.OnMailFrom::emailAddress)
        .orNull()

    assertNotNull(maybeEmailAddress, "No email address was parsed")
  }

  @Test
  @Tag(Isolated)
  fun assert_mail_from_command_spaces_are_stripped() {
    val mailFromWithSingleParameter = parseCommand("   MAIL FROM:    mailbox@domain.com   ")

    val maybeEmailAddress = mailFromWithSingleParameter
      .flatMap { toEvent<Event.OnMailFrom>(it) }
      .map(Event.OnMailFrom::emailAddress)

    succeeds(mailFromWithSingleParameter, "No email address was parsed")
    matches(maybeEmailAddress, EmailAddress::mailbox, "mailbox", "Unexpected mailbox")
    matches(maybeEmailAddress, EmailAddress::domainName, "domain.com", "Unexpected hostname")
    assertEmailAddressMatchesRules(maybeEmailAddress.map(EmailAddress::address).getOrElse { "" })
  }

  @Test
  @Tag(Isolated)
  fun assert_mail_from_command_case_is_ignored() {
    val mailFromWithSingleParameter = parseCommand("mAiL fRoM: mailbox@domain.com")

    isType(mailFromWithSingleParameter, Event.OnMailFrom::class)
  }
  // endregion

  // region RCPT TO
  @Test
  @Tag(Isolated)
  fun assert_rcpt_to_command_recognized_with_exactly_one_parameter() {
    val mailFromWithoutParameter = parseCommand("RCPT TO")
    val mailFromWithSingleParameter = parseCommand(ValidRcptToCommand)
    val mailFromWithMultipleParameters = parseCommand("RCPT TO: yes@no.com some@thing.com")

    fails(mailFromWithoutParameter, "Parsing parameterless RCPT TO should fail")
    isType(mailFromWithSingleParameter, Event.OnRcptTo::class)
    fails(
      mailFromWithMultipleParameters,
      "Parsing RCPT TO with more than a single parameter should fail"
    )
  }

  @Test
  @Tag(Isolated)
  fun assert_rcpt_to_command_accepts_single_email_address_parameter() {
    val mailFromWithSingleParameter = parseCommand(ValidRcptToCommand)

    isType(mailFromWithSingleParameter, Event.OnRcptTo::class)
  }

  @Test
  @Tag(Isolated)
  fun assert_rcpt_to_command_spaces_are_stripped() {
    val mailFromWithSingleParameter = parseCommand("   RCPT TO:    mailbox@domain.com   ")

    val errorOrMailAddress = mailFromWithSingleParameter
      .flatMap { toEvent<Event.OnRcptTo>(it) }
      .map(Event.OnRcptTo::emailAddress)

    isType(mailFromWithSingleParameter, Event.OnRcptTo::class)
    matches(errorOrMailAddress, EmailAddress::mailbox, "mailbox", "Unexpected mailbox")
    matches(errorOrMailAddress, EmailAddress::domainName, "domain.com", "Unexpected hostname")
    assertEmailAddressMatchesRules(errorOrMailAddress.map(EmailAddress::address).getOrElse { "" })
  }

  @Test
  @Tag(Isolated)
  fun assert_rcpt_to_command_case_is_ignored() {
    val mailFromWithSingleParameter = parseCommand("rCpT tO: mailbox@domain.com")

    isType(mailFromWithSingleParameter, Event.OnRcptTo::class)
  }
  // endregion

  // region DATA
  @Test
  @Tag(Isolated)
  fun assert_data_command_recognized_with_exactly_one_parameter() {
    val dataWithoutParameter = parseCommand(ValidDataCommand)
    val dataWithSingleParameter = parseCommand("DATA infi.nl")
    val dataWithMultipleParameters = parseCommand("DATA infi.nl nu.nl")

    isType(dataWithoutParameter, Event.OnData::class)
    fails(dataWithSingleParameter, "Parsing DATA with a single parameter should fail")
    fails(dataWithMultipleParameters, "Parsing DATA with more than a single parameter should fail")
  }

  @Test
  @Tag(Isolated)
  fun assert_data_command_spaces_are_stripped() {
    val dataWithoutParameter = parseCommand("   DATA    ")

    isType(dataWithoutParameter, Event.OnData::class)
  }

  @Test
  @Tag(Isolated)
  fun assert_data_command_case_is_ignored() {
    val dataWithoutParameters = parseCommand("dAtA")

    isType(dataWithoutParameters, Event.OnData::class)
  }
  // endregion

  // region QUIT
  @Test
  @Tag(Isolated)
  fun assert_quit_command_recognized_with_exactly_one_parameter() {
    val quitWithoutParameter = parseCommand(ValidQuitCommand)
    val quitWithSingleParameter = parseCommand("QUIT infi.nl")
    val quitWithMultipleParameters = parseCommand("QUIT infi.nl nu.nl")

    isType(quitWithoutParameter, Event.OnQuit::class)
    fails(quitWithSingleParameter, "Parsing QUIT with a single parameter should fail")
    fails(quitWithMultipleParameters, "Parsing QUIT with more than a single parameter should fail")
  }

  @Test
  @Tag(Isolated)
  fun assert_quit_command_spaces_are_stripped() {
    val quitWithoutParameter = parseCommand("   QUIT    ")

    isType(quitWithoutParameter, Event.OnQuit::class)
  }

  @Test
  @Tag(Isolated)
  fun assert_quit_command_case_is_ignored() {
    val quitWithoutParameters = parseCommand("qUiT")

    isType(quitWithoutParameters, Event.OnQuit::class)
  }
  // endregion
}
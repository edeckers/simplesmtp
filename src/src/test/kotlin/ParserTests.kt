import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import io.deckers.smtpjer.parsers.EmailAddress
import io.deckers.smtpjer.state_machine.Event
import io.deckers.smtpjer.parsers.parseCommand
import org.junit.jupiter.api.Test
import kotlin.test.*

private const val ValidEhloCommand = "EHLO infi.nl"
private const val ValidHeloCommand = "HELO infi.nl"
private const val ValidMailFromCommand = "MAIL FROM: mailbox@domain.com"
private const val ValidRcptToCommand = "RCPT TO: mailbox@domain.com"
private const val ValidDataCommand = "DATA"
private const val ValidQuitCommand = "QUIT"

class ParserTests {
  // region E-mail address
  @Test
  fun assert_email_address_validates_input() {
    val errorOrEmailAddress = EmailAddress.parse("mailbox@domain.com")

    val maybeEmailAddress = errorOrEmailAddress.orNull()

    assertNotNull(maybeEmailAddress, "No email address was parsed")
    assertEquals("mailbox", maybeEmailAddress.mailbox, "Unexpected mailbox")
    assertEquals("domain.com", maybeEmailAddress.domainName, "Unexpected hostname")
    assert_email_address_matches_rules(maybeEmailAddress.address)
  }
  // endregion

  // region EHLO
  @Test
  fun assert_ehlo_command_recognized_with_exactly_one_parameter() {
    val ehloWithoutParameter = parseCommand("EHLO")
    val ehloWithSingleParameter = parseCommand(ValidEhloCommand)
    val ehloWithMultipleParameters = parseCommand("EHLO infi.nl nu.nl")

    assertTrue(ehloWithoutParameter.isLeft(), "Parsing parameterless EHLO should fail")
    assertEquals(Event.OnEhlo::class, ehloWithSingleParameter.getOrElse { }::class, "Expected OnEhlo")
    assertTrue(ehloWithMultipleParameters.isLeft(), "Parsing EHLO with more than a single parameter should fail")
  }

  @Test
  fun assert_ehlo_command_accepts_single_domain_parameter() {
    val ehloWithSingleParameter = parseCommand(ValidEhloCommand)

    val domainName = ehloWithSingleParameter
      .flatMap { toEvent<Event.OnEhlo>(it) }
      .map { e -> e.domain }
      .orNull()

    assertNotNull(domainName, "No domainName was parsed")
    assert_domainName_matches_rules(domainName.name)
  }

  @Test
  fun assert_ehlo_command_spaces_are_stripped() {
    val ehloWithSingleParameter = parseCommand("   EHLO    in-fi.nl   ")

    val domainName = ehloWithSingleParameter
      .flatMap { toEvent<Event.OnEhlo>(it) }
      .map { e -> e.domain }
      .orNull()

    assertNotNull(domainName, "No domainName was parsed")
    assert_domainName_matches_rules(domainName.name)
    assertEquals("in-fi.nl", domainName.name)
  }

  @Test
  fun assert_ehlo_command_case_is_ignored() {
    val ehloWithSingleParameter = parseCommand("eHlO infi.nl")

    val errorOrEvent =
      ehloWithSingleParameter
        .flatMap { toEvent<Event.OnEhlo>(it) }

    val maybeEvent = errorOrEvent.orNull()

    val maybeDomainName =
      errorOrEvent
        .map { e -> e.domain }
        .orNull()

    assertNotNull(maybeEvent, "Event should be OnEhlo")
    assertEquals("infi.nl", maybeDomainName?.name)
  }
// endregion

  // region HELO
  @Test
  fun assert_helo_command_recognized_with_exactly_one_parameter() {
    val heloWithoutParameter = parseCommand("HELO")
    val heloWithSingleParameter = parseCommand(ValidHeloCommand)
    val heloWithMultipleParameters = parseCommand("HELO infi.nl nu.nl")

    assertTrue(heloWithoutParameter.isLeft(), "Parsing parameterless HELO should fail")
    assertEquals(Event.OnHelo::class, heloWithSingleParameter.getOrElse { }::class, "Expected OnHelo")
    assertTrue(heloWithMultipleParameters.isLeft(), "Parsing HELO with more than a single parameter should fail")
  }

  @Test
  fun assert_helo_command_accepts_single_domain_parameter() {
    val heloWithSingleParameter = parseCommand(ValidHeloCommand)

    val domainName = heloWithSingleParameter
      .flatMap { toEvent<Event.OnHelo>(it) }
      .map { e -> e.domain }
      .orNull()

    assertNotNull(domainName, "No domainName was parsed")
    assert_domainName_matches_rules(domainName.name)
  }

  @Test
  fun assert_helo_command_spaces_are_stripped() {
    val heloWithSingleParameter = parseCommand("   HELO    in-fi.nl   ")

    val domainName = heloWithSingleParameter
      .flatMap { toEvent<Event.OnHelo>(it) }
      .map { e -> e.domain }
      .orNull()

    assertNotNull(domainName, "No domainName was parsed")
    assert_domainName_matches_rules(domainName.name)
    assertEquals("in-fi.nl", domainName.name)
  }

  @Test
  fun assert_helo_command_case_is_ignored() {
    val heloWithSingleParameter = parseCommand("helO infi.nl")

    val errorOrEvent =
      heloWithSingleParameter
        .flatMap { toEvent<Event.OnHelo>(it) }

    val maybeEvent = errorOrEvent.orNull()

    val maybeDomainName =
      errorOrEvent
        .map { e -> e.domain }
        .orNull()

    assertNotNull(maybeEvent, "Event should be OnHelo")
    assertEquals("infi.nl", maybeDomainName?.name)
  }
  // endregion

  // region MAIL FROM
  @Test
  fun assert_mail_from_command_recognized_with_exactly_one_parameter() {
    val mailFromWithoutParameter = parseCommand("MAIL FROM")
    val mailFromWithSingleParameter = parseCommand(ValidMailFromCommand)
    val mailFromWithMultipleParameters = parseCommand("MAIL FROM: yes@no.com some@thing.com")

    assertTrue(mailFromWithoutParameter.isLeft(), "Parsing parameterless MAIL FROM should fail")
    assertEquals(Event.OnMailFrom::class, mailFromWithSingleParameter.getOrElse { }::class, "Expected OnMailFrom")
    assertTrue(
      mailFromWithMultipleParameters.isLeft(),
      "Parsing MAIL FROM with more than a single parameter should fail"
    )
  }

  @Test
  fun assert_mail_from_command_accepts_single_email_address_parameter() {
    val mailFromWithSingleParameter = parseCommand(ValidMailFromCommand)

    val maybeEmailAddress =
      mailFromWithSingleParameter
        .flatMap { toEvent<Event.OnMailFrom>(it) }
        .map { e -> e.emailAddress }
        .orNull()

    assertNotNull(maybeEmailAddress, "No email address was parsed")
  }

  @Test
  fun assert_mail_from_command_spaces_are_stripped() {
    val mailFromWithSingleParameter = parseCommand("   MAIL FROM:    mailbox@domain.com   ")

    val maybeEmailAddress = mailFromWithSingleParameter
      .flatMap { toEvent<Event.OnMailFrom>(it) }
      .map { it.emailAddress }
      .orNull()

    assertNotNull(maybeEmailAddress, "No email address was parsed")
    assertEquals("mailbox", maybeEmailAddress.mailbox, "Unexpected mailbox")
    assertEquals("domain.com", maybeEmailAddress.domainName, "Unexpected hostname")
  }

  @Test
  fun assert_mail_from_command_case_is_ignored() {
    val mailFromWithSingleParameter = parseCommand("mAiL fRoM: mailbox@domain.com")

    val errorOrEvent =
      mailFromWithSingleParameter
        .flatMap { toEvent<Event.OnMailFrom>(it) }

    val maybeEvent = errorOrEvent.orNull()

    assertNotNull(maybeEvent, "Event should be OnMailFrom")
  }
  // endregion

  // region RCPT TO
  @Test
  fun assert_rcpt_to_command_recognized_with_exactly_one_parameter() {
    val mailFromWithoutParameter = parseCommand("RCPT TO")
    val mailFromWithSingleParameter = parseCommand(ValidRcptToCommand)
    val mailFromWithMultipleParameters = parseCommand("RCPT TO: yes@no.com some@thing.com")

    assertTrue(mailFromWithoutParameter.isLeft(), "Parsing parameterless RCPT TO should fail")
    assertEquals(Event.OnRcptTo::class, mailFromWithSingleParameter.getOrElse { }::class, "Expected OnRcptTo")
    assertTrue(
      mailFromWithMultipleParameters.isLeft(),
      "Parsing RCPT TO with more than a single parameter should fail"
    )
  }

  @Test
  fun assert_rcpt_to_command_accepts_single_email_address_parameter() {
    val mailFromWithSingleParameter = parseCommand(ValidRcptToCommand)

    val maybeEmailAddress =
      mailFromWithSingleParameter
        .flatMap { toEvent<Event.OnRcptTo>(it) }
        .map { e -> e.emailAddress }
        .orNull()

    assertNotNull(maybeEmailAddress, "No email address was parsed")
  }

  @Test
  fun assert_rcpt_to_command_spaces_are_stripped() {
    val mailFromWithSingleParameter = parseCommand("   RCPT TO:    mailbox@domain.com   ")

    val maybeEmailAddress = mailFromWithSingleParameter
      .flatMap { toEvent<Event.OnRcptTo>(it) }
      .map { it.emailAddress }
      .orNull()

    assertNotNull(maybeEmailAddress, "No email address was parsed")
    assertEquals("mailbox", maybeEmailAddress.mailbox, "Unexpected mailbox")
    assertEquals("domain.com", maybeEmailAddress.domainName, "Unexpected hostname")
  }

  @Test
  fun assert_rcpt_to_command_case_is_ignored() {
    val mailFromWithSingleParameter = parseCommand("rCpT tO: mailbox@domain.com")

    val errorOrEvent =
      mailFromWithSingleParameter
        .flatMap { toEvent<Event.OnRcptTo>(it) }

    val maybeEvent = errorOrEvent.orNull()

    assertNotNull(maybeEvent, "Event should be OnRcptTo")
  }
  // endregion

  // region DATA
  @Test
  fun assert_data_command_recognized_with_exactly_one_parameter() {
    val dataWithoutParameter = parseCommand(ValidDataCommand)
    val dataWithSingleParameter = parseCommand("DATA infi.nl")
    val dataWithMultipleParameters = parseCommand("DATA infi.nl nu.nl")

    assertEquals(Event.OnData::class, dataWithoutParameter.getOrElse { }::class, "Parsing parameterless DATA should succeed")
    assertTrue(dataWithSingleParameter.isLeft(), "Parsing DATA with a single parameter should fail")
    assertTrue(dataWithMultipleParameters.isLeft(), "Parsing DATA with more than a single parameter should fail")
  }

  @Test
  fun assert_data_command_spaces_are_stripped() {
    val dataWithoutParameter = parseCommand("   DATA    ")

    val maybeData = dataWithoutParameter
      .flatMap { toEvent<Event.OnData>(it) }
      .orNull()

    assertNotNull(maybeData, "No DATA command was parsed")
  }

  @Test
  fun assert_data_command_case_is_ignored() {
    val dataWithoutParameters = parseCommand("dAtA")

    val maybeData =
      dataWithoutParameters
        .flatMap { toEvent<Event.OnData>(it) }
        .orNull()

    assertNotNull(maybeData, "Event should be OnData")
  }
  // endregion

  // region QUIT
  @Test
  fun assert_quit_command_recognized_with_exactly_one_parameter() {
    val quitWithoutParameter = parseCommand(ValidQuitCommand)
    val quitWithSingleParameter = parseCommand("QUIT infi.nl")
    val quitWithMultipleParameters = parseCommand("QUIT infi.nl nu.nl")

    assertEquals(Event.OnQuit::class, quitWithoutParameter.getOrElse { }::class, "Parsing parameterless QUIT should succeed")
    assertTrue(quitWithSingleParameter.isLeft(), "Parsing QUIT with a single parameter should fail")
    assertTrue(quitWithMultipleParameters.isLeft(), "Parsing QUIT with more than a single parameter should fail")
  }

  @Test
  fun assert_quit_command_spaces_are_stripped() {
    val quitWithoutParameter = parseCommand("   QUIT    ")

    val maybeQuit = quitWithoutParameter
      .flatMap { toEvent<Event.OnQuit>(it) }
      .orNull()

    assertNotNull(maybeQuit, "No QUIT command was parsed")
  }

  @Test
  fun assert_quit_command_case_is_ignored() {
    val quitWithoutParameters = parseCommand("qUiT")

    val maybeQuit =
      quitWithoutParameters
        .flatMap { toEvent<Event.OnQuit>(it) }
        .orNull()

    assertNotNull(maybeQuit, "Event should be OnQuit")
  }
  // endregion

  private fun assert_domainName_matches_rules(domainName: String) {
    assertTrue(domainName.isNotEmpty(), "Domain name length should be at least one character long")
    assertTrue(domainName.matches("[a-zA-Z0-9.-]*".toRegex()), "Domain name contains unexpected characters")
    assertNotSame('-', domainName[0], "Domain cannot start with hyphen")
  }

  private fun assert_email_address_matches_rules(emailAddress: String) {
    assertTrue(emailAddress.length >= 3, "E-mail address length should be at least three character long")
    assertTrue(emailAddress.matches("[a-zA-Z0-9.-@]*".toRegex()), "E-mail address contains unexpected characters")
    assertTrue(emailAddress.contains('@'), "E-mail address must contain @")
  }

  private inline fun <reified T> toEvent(e: Event): Either<Throwable, T> =
    if (e is T) Either.Right(e) else Either.Left(Error(""))
}
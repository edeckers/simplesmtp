/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
 import arrow.core.flatMap
import utils.*
import io.deckers.smtpjer.parsers.EmailAddress
import io.deckers.smtpjer.state_machines.Event
import io.deckers.smtpjer.parsers.parseCommand
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.*

class ParserTests {
  // region E-mail address
  @Test
  @Tag(Isolated)
  fun assert_email_address_validates_input() =
    with(EmailAddress.parse("mailbox@domain.com")) {
      succeeds("No email address was parsed")
      matches(EmailAddress::mailbox, "mailbox", "Unexpected mailbox")
      matches(EmailAddress::domainName, "domain.com", "Unexpected hostname")

      assertEmailAddressMatchesRules(map(EmailAddress::address).orNull())
    }

  @Test
  @Tag(Isolated)
  fun assert_email_address_accepts_brackets() =
    with(EmailAddress.parse("<mailbox@domain.com>")) {
      succeeds("No email address was parsed")
      matches(EmailAddress::mailbox, "mailbox", "Unexpected mailbox")
      matches(EmailAddress::domainName, "domain.com", "Unexpected hostname")

      assertEmailAddressMatchesRules(map(EmailAddress::address).orNull())
    }
  // endregion

  // region EHLO
  @Test
  @Tag(Isolated)
  fun assert_ehlo_command_recognized_with_exactly_one_parameter() {
    val ehloWithoutParameter = parseCommand("EHLO")
    val ehloWithSingleParameter = parseCommand(ValidEhloCommand)
    val ehloWithMultipleParameters = parseCommand("EHLO infi.nl nu.nl")

    ehloWithoutParameter.fails("Parsing parameterless EHLO should fail")
    ehloWithSingleParameter.isType(Event.OnEhlo::class)
    ehloWithMultipleParameters.fails("Parsing EHLO with more than a single parameter should fail")
  }

  @Test
  @Tag(Isolated)
  fun assert_ehlo_command_accepts_single_domain_parameter() {
    val ehloWithSingleParameter = parseCommand(ValidEhloCommand)

    val maybeDomainName = ehloWithSingleParameter
      .flatMap { toEvent<Event.OnEhlo>(it) }
      .map(Event.OnEhlo::domain)
      .orNull()

    ehloWithSingleParameter.isType(Event.OnEhlo::class)
    assertDomainNameMatchesRules(maybeDomainName?.name)
  }

  @Test
  @Tag(Isolated)
  fun assert_ehlo_command_spaces_are_stripped() {
    val ehloWithSingleParameter = parseCommand("   EHLO    in-fi.nl   ")

    val maybeDomainName = ehloWithSingleParameter
      .flatMap { toEvent<Event.OnEhlo>(it) }
      .map(Event.OnEhlo::domain)
      .orNull()

    ehloWithSingleParameter.isType(Event.OnEhlo::class)
    assertDomainNameMatchesRules(maybeDomainName?.name)
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

    ehloWithSingleParameter.isType(Event.OnEhlo::class)
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

    heloWithoutParameter.fails("Parsing parameterless HELO should fail")
    heloWithSingleParameter.isType(Event.OnHelo::class)
    heloWithMultipleParameters.fails("Parsing HELO with more than a single parameter should fail")
  }

  @Test
  @Tag(Isolated)
  fun assert_helo_command_accepts_single_domain_parameter() {
    val heloWithSingleParameter = parseCommand(ValidHeloCommand)

    val maybeDomainName = heloWithSingleParameter
      .flatMap { toEvent<Event.OnHelo>(it) }
      .map(Event.OnHelo::domain)
      .orNull()

    heloWithSingleParameter.isType(Event.OnHelo::class)
    assertDomainNameMatchesRules(maybeDomainName?.name)
  }

  @Test
  @Tag(Isolated)
  fun assert_helo_command_spaces_are_stripped() {
    val heloWithSingleParameter = parseCommand("   HELO    in-fi.nl   ")

    val maybeDomainName = heloWithSingleParameter
      .flatMap { toEvent<Event.OnHelo>(it) }
      .map(Event.OnHelo::domain)
      .orNull()

    heloWithSingleParameter.isType(Event.OnHelo::class)
    assertDomainNameMatchesRules(maybeDomainName?.name)
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

    heloWithSingleParameter.isType(Event.OnHelo::class)
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

    mailFromWithoutParameter.fails("Parsing parameterless MAIL FROM should fail")
    mailFromWithSingleParameter.isType(Event.OnMailFrom::class)
    mailFromWithMultipleParameters.fails("Parsing MAIL FROM with more than a single parameter should fail")
  }

  @Test
  @Tag(Isolated)
  fun assert_mail_from_command_accepts_single_email_address_parameter() {
    val mailFromWithSingleParameter = parseCommand(ValidMailFromCommand)

    mailFromWithSingleParameter.isType(Event.OnMailFrom::class)
  }

  @Test
  @Tag(Isolated)
  fun assert_mail_from_command_spaces_are_stripped() {
    val mailFromWithSingleParameter = parseCommand("   MAIL FROM:    mailbox@domain.com   ")

    val maybeEmailAddress = mailFromWithSingleParameter
      .flatMap { toEvent<Event.OnMailFrom>(it) }
      .map(Event.OnMailFrom::emailAddress)

    mailFromWithSingleParameter.isType(Event.OnMailFrom::class)
    maybeEmailAddress.matches(EmailAddress::mailbox, "mailbox", "Unexpected mailbox")
    maybeEmailAddress.matches(EmailAddress::domainName, "domain.com", "Unexpected hostname")
  }

  @Test
  @Tag(Isolated)
  fun assert_mail_from_command_case_is_ignored() {
    val mailFromWithSingleParameter = parseCommand("mAiL fRoM: mailbox@domain.com")

    mailFromWithSingleParameter.isType(Event.OnMailFrom::class)
  }
  // endregion

  // region RCPT TO
  @Test
  @Tag(Isolated)
  fun assert_rcpt_to_command_recognized_with_exactly_one_parameter() {
    val mailFromWithoutParameter = parseCommand("RCPT TO")
    val mailFromWithSingleParameter = parseCommand(ValidRcptToCommand)
    val mailFromWithMultipleParameters = parseCommand("RCPT TO: yes@no.com some@thing.com")

    mailFromWithoutParameter.fails("Parsing parameterless RCPT TO should fail")
    mailFromWithSingleParameter.isType(Event.OnRcptTo::class)
    mailFromWithMultipleParameters.fails("Parsing RCPT TO with more than a single parameter should fail")
  }

  @Test
  @Tag(Isolated)
  fun assert_rcpt_to_command_accepts_single_email_address_parameter() {
    val mailFromWithSingleParameter = parseCommand(ValidRcptToCommand)

    mailFromWithSingleParameter.isType(Event.OnRcptTo::class)
  }

  @Test
  @Tag(Isolated)
  fun assert_rcpt_to_command_spaces_are_stripped() {
    val mailFromWithSingleParameter = parseCommand("   RCPT TO:    mailbox@domain.com   ")

    val errorOrMailAddress = mailFromWithSingleParameter
      .flatMap { toEvent<Event.OnRcptTo>(it) }
      .map(Event.OnRcptTo::emailAddress)

    mailFromWithSingleParameter.isType(Event.OnRcptTo::class)
    errorOrMailAddress.matches(EmailAddress::mailbox, "mailbox", "Unexpected mailbox")
    errorOrMailAddress.matches(EmailAddress::domainName, "domain.com", "Unexpected hostname")
  }

  @Test
  @Tag(Isolated)
  fun assert_rcpt_to_command_case_is_ignored() {
    val mailFromWithSingleParameter = parseCommand("rCpT tO: mailbox@domain.com")

    mailFromWithSingleParameter.isType(Event.OnRcptTo::class)
  }
  // endregion

  // region DATA
  @Test
  @Tag(Isolated)
  fun assert_data_command_recognized_with_exactly_one_parameter() {
    val dataWithoutParameter = parseCommand(ValidDataCommand)
    val dataWithSingleParameter = parseCommand("DATA infi.nl")
    val dataWithMultipleParameters = parseCommand("DATA infi.nl nu.nl")

    dataWithoutParameter.isType(Event.OnData::class)
    dataWithSingleParameter.fails("Parsing DATA with a single parameter should fail")
    dataWithMultipleParameters.fails("Parsing DATA with more than a single parameter should fail")
  }

  @Test
  @Tag(Isolated)
  fun assert_data_command_spaces_are_stripped() {
    val dataWithoutParameter = parseCommand("   DATA    ")

    dataWithoutParameter.isType(Event.OnData::class)
  }

  @Test
  @Tag(Isolated)
  fun assert_data_command_case_is_ignored() {
    val dataWithoutParameters = parseCommand("dAtA")

    dataWithoutParameters.isType(Event.OnData::class)
  }
  // endregion

  // region QUIT
  @Test
  @Tag(Isolated)
  fun assert_quit_command_recognized_with_exactly_one_parameter() {
    val quitWithoutParameter = parseCommand(ValidQuitCommand)
    val quitWithSingleParameter = parseCommand("QUIT infi.nl")
    val quitWithMultipleParameters = parseCommand("QUIT infi.nl nu.nl")

    quitWithoutParameter.isType(Event.OnQuit::class)
    quitWithSingleParameter.fails("Parsing QUIT with a single parameter should fail")
    quitWithMultipleParameters.fails("Parsing QUIT with more than a single parameter should fail")
  }

  @Test
  @Tag(Isolated)
  fun assert_quit_command_spaces_are_stripped() {
    val quitWithoutParameter = parseCommand("   QUIT    ")

    quitWithoutParameter.isType(Event.OnQuit::class)
  }

  @Test
  @Tag(Isolated)
  fun assert_quit_command_case_is_ignored() {
    val quitWithoutParameters = parseCommand("qUiT")

    quitWithoutParameters.isType(Event.OnQuit::class)
  }
  // endregion
}
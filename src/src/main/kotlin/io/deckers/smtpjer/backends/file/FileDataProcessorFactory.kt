/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.smtpjer.backends.file

import io.deckers.smtpjer.parsers.EmailAddress
import io.deckers.smtpjer.backends.DataProcessor
import io.deckers.smtpjer.backends.DataProcessorFactory
import io.deckers.smtpjer.parsers.DomainName

class FileDataProcessorFactory : DataProcessorFactory {
  override fun create(domain: DomainName, from: EmailAddress, to: EmailAddress): DataProcessor =
    FileDataProcessor(to)
}
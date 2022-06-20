/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.smtpjer.backends

import io.deckers.smtpjer.parsers.DomainName
import io.deckers.smtpjer.parsers.EmailAddress

interface DataProcessorFactory {
  fun create(domain: DomainName, from: EmailAddress, to: EmailAddress) : DataProcessor
}
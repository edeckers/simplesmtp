package io.deckers.smtpjer.backends.proxy

import io.deckers.smtpjer.parsers.EmailAddress
import io.deckers.smtpjer.backends.DataProcessor
import io.deckers.smtpjer.backends.DataProcessorFactory
import io.deckers.smtpjer.parsers.DomainName
import java.net.InetAddress

class PoorMansProxyFactory(private val host: InetAddress, private val port: Int) : DataProcessorFactory {
  override fun create(domain: DomainName, from: EmailAddress, to: EmailAddress): DataProcessor =
    PoorMansProxy(host, port, domain, from, to)
}
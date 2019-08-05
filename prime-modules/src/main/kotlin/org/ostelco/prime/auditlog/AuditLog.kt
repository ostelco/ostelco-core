package org.ostelco.prime.auditlog

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ostelco.prime.model.CustomerActivity
import org.ostelco.prime.model.Identity
import org.ostelco.prime.model.Severity
import org.ostelco.prime.model.Severity.ERROR
import org.ostelco.prime.model.Severity.INFO
import org.ostelco.prime.model.Severity.WARN
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.AuditLogStore
import org.ostelco.prime.storage.ClientDataSource
import java.time.Instant

object AuditLog {

    private val auditLogger by lazy { getResource<AuditLogStore>() }

    private val dataSource by lazy { getResource<ClientDataSource>() }

    fun info(customerId: String, message: String) = log(INFO, customerId, message)

    fun warn(customerId: String, message: String) = log(WARN, customerId, message)

    fun error(customerId: String, message: String) = log(ERROR, customerId, message)

    fun info(identity: Identity, message: String) = log(INFO, identity, message)

    fun warn(identity: Identity, message: String) = log(WARN, identity, message)

    fun error(identity: Identity, message: String) = log(ERROR, identity, message)

    private fun log(severity: Severity, identity: Identity, message: String) {
        val now = Instant.now()
        CoroutineScope(Dispatchers.Default).launch {
            dataSource.getCustomer(identity).map { customer ->
                auditLogger.logCustomerActivity(
                        customerId = customer.id,
                        customerActivity = CustomerActivity(
                                timestamp = now.toEpochMilli(),
                                severity = severity,
                                message = message
                        )
                )
            }
        }
    }

    private fun log(severity: Severity, customerId: String, message: String) {
        val now = Instant.now()
        CoroutineScope(Dispatchers.Default).launch {
            auditLogger.logCustomerActivity(
                    customerId = customerId,
                    customerActivity = CustomerActivity(
                            timestamp = now.toEpochMilli(),
                            severity = severity,
                            message = message
                    )
            )
        }
    }
}
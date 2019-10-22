package org.ostelco.prime.storage.documentstore

import arrow.core.Either
import arrow.core.getOrElse
import com.google.cloud.Timestamp
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.ApplicationToken
import org.ostelco.prime.model.Severity
import org.ostelco.prime.storage.DocumentStore
import org.ostelco.prime.storage.documentstore.ConfigRegistry.config
import org.ostelco.prime.store.datastore.EntityStore
import java.util.*

class DocumentDataStore : DocumentStore by DocumentDataStoreSingleton

object DocumentDataStoreSingleton : DocumentStore {

    private val logger by getLogger()

    private const val PARENT_KIND = "org.ostelco.prime.model.Customer"

    private val notificationTokenStore = EntityStore(
            entityClass = ApplicationToken::class,
            type = config.storeType,
            namespace = config.namespace
    )

    private val customerActivityStore = EntityStore(
            entityClass = CustomerActivity::class,
            type = config.storeType,
            namespace = config.namespace
    )

    override fun getNotificationTokens(customerId: String): Collection<ApplicationToken> = notificationTokenStore
            .fetchAll(parentKind = PARENT_KIND, parentKeyString = customerId)
            .getOrElse { emptyList() }

    override fun addNotificationToken(customerId: String, token: ApplicationToken): Boolean {
        // Remove any other entries with the same token.
        getNotificationTokens(customerId).forEach {
            if (it.tokenType == token.tokenType && it.token == token.token && it.applicationID != token.applicationID) {
                removeNotificationToken(customerId, it.applicationID)
            }
        }
        return notificationTokenStore.put(
                token,
                token.applicationID,
                Pair(PARENT_KIND, customerId)
        ).isRight()
    }

    override fun getNotificationToken(customerId: String, applicationID: String): ApplicationToken? {
        return notificationTokenStore.fetch(applicationID, Pair(PARENT_KIND, customerId))
                .getOrElse { null }
    }

    override fun removeNotificationToken(customerId: String, applicationID: String): Boolean {
        return notificationTokenStore.delete(applicationID, Pair(PARENT_KIND, customerId)).isRight()
    }

    override fun logCustomerActivity(
            customerId: String,
            customerActivity: org.ostelco.prime.model.CustomerActivity) {

        customerActivityStore.add(
                entity = CustomerActivity(
                        timestamp = Timestamp.of(Date(customerActivity.timestamp)),
                        severity = customerActivity.severity.name,
                        message = customerActivity.message
                ),
                keyString = null,
                parents = *arrayOf(Pair(PARENT_KIND, customerId))
        )
    }

    override fun getCustomerActivityHistory(
            customerId: String): Either<String, Collection<org.ostelco.prime.model.CustomerActivity>> {

        return customerActivityStore.fetchAll(
                parentKind = PARENT_KIND,
                parentKeyString = customerId)
                .map { customerActivityList ->
                    customerActivityList.map { customerActivity ->
                        org.ostelco.prime.model.CustomerActivity(
                                timestamp = customerActivity.timestamp.toDate().time,
                                severity = Severity.valueOf(customerActivity.severity),
                                message = customerActivity.message
                        )
                    }.sortedByDescending { it.timestamp }
                }
                .mapLeft {
                    logger.error("Failed to fetch customer activity history", it)
                    it.message ?: "Exception without message."
                }
    }
}
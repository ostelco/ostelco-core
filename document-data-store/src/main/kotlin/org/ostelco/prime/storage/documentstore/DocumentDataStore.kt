package org.ostelco.prime.storage.documentstore

import arrow.core.getOrElse
import org.ostelco.prime.model.ApplicationToken
import org.ostelco.prime.storage.DocumentStore
import org.ostelco.prime.storage.documentstore.ConfigRegistry.config
import org.ostelco.prime.store.datastore.EntityStore

class DocumentDataStore : DocumentStore by DocumentDataStoreSingleton

object DocumentDataStoreSingleton : DocumentStore {

    private const val PARENT_KIND = "Customer"

    private val notificationTokenStore = EntityStore(
            entityClass = ApplicationToken::class.java,
            type = config.storeType,
            namespace = config.namespace
    )

    override fun getNotificationTokens(customerId: String): Collection<ApplicationToken> = notificationTokenStore
            .fetchAll(parentKind= PARENT_KIND, parentKeyString = customerId)
            .getOrElse { emptyList() }

    override fun addNotificationToken(customerId: String, token: ApplicationToken): Boolean {
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
}
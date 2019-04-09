package org.ostelco.prime.paymentprocessor.subscribers

import arrow.core.Try
import com.google.cloud.NoCredentials
import com.google.cloud.datastore.*
import com.google.cloud.datastore.testing.LocalDatastoreHelper
import com.google.cloud.http.HttpTransportOptions
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.protobuf.ByteString
import com.stripe.model.Event
import com.stripe.net.ApiResource.GSON
import org.ostelco.prime.getLogger
import org.ostelco.prime.paymentprocessor.ConfigRegistry
import org.ostelco.prime.pubsub.PubSubSubscriber


class StoreStripeEvent : PubSubSubscriber(
        subscription = ConfigRegistry.config.stripeEventStoreSubscriptionId,
        topic = ConfigRegistry.config.stripeEventTopicId,
        project = ConfigRegistry.config.projectId) {

    private val logger by getLogger()

    /* GCP datastore. */
    private val entityStore = EntityStore(type = ConfigRegistry.config.storeType,
            namespace = ConfigRegistry.config.namespace)

    override fun handler(message: ByteString, consumer: AckReplyConsumer) =
            Try {
                GSON.fromJson(message.toStringUtf8(), Event::class.java)
            }.fold(
                    ifSuccess = { event ->
                        Try {
                            /* TODO: Update 'data-store' to support some sort of annotation
                                     to mark fields to be excluded from indexing. */
                            entityStore.add(StripeEvent(event.type,
                                    event.account,
                                    event.created,
                                    StringValue.newBuilder(message.toStringUtf8())
                                            .setExcludeFromIndexes(true)
                                            .build()))
                        }.toEither()
                                .mapLeft {
                                    logger.error("Failed to store Stripe event {}: {}",
                                            event.id, it)
                                }
                        consumer.ack()
                    },
                    ifFailure = {
                        logger.error("Failed to decode Stripe event for logging and error reporting: {}",
                                it)
                        consumer.ack()
                    }
            )
}

data class StripeEvent(val type: String,
                       val account: String?,
                       val created: Long,
                       val json: StringValue)

/**
 * Temporary DataStore function for Stripe events.
 *
 * The 'org.ostelco.prime.store.datastore.EntityStore' can't currently handle
 * store/fetch of GCP datastore 'Value<*>' type, such as 'StringValue' etc.,
 * as Jackson fails at (de)serializing such objects.
 *
 * TODO: Fix 'org.ostelco.prime.store.datastore.EntityStore' handling of
 *       'Value<*' types, and update to use the updated 'entitystore' to
 *       store Stripe events.
 */
class EntityStore(type: String = "inmemory-emulator",
                  namespace: String = "") {
    private val logger by getLogger()

    private val keyFactory: KeyFactory
    private val datastore: Datastore

    init {
        datastore = when (type) {
            "inmemory-emulator" -> {
                logger.info("Created 'in-memory' emulator instance of datastore client")
                val localDatastoreHelper = LocalDatastoreHelper.create(1.0)
                localDatastoreHelper.start()
                localDatastoreHelper.options
            }
            "emulator" -> {
                logger.info("Created emulator instance of datastore client")
                DatastoreOptions
                        .newBuilder()
                        .setHost("localhost:9090")
                        .setCredentials(NoCredentials.getInstance())
                        .setTransportOptions(HttpTransportOptions.newBuilder().build())
                        .build()
            }
            else -> {
                logger.info("Created default instance of datastore client")
                DatastoreOptions
                        .newBuilder()
                        .setNamespace(namespace)
                        .build()
            }
        }.service
        keyFactory = datastore.newKeyFactory().setKind(StripeEvent::class.java.name)
    }

    fun fetch(key: Key): StripeEvent {
        val fullEntity = datastore.fetch(key).single()

        return StripeEvent(
                type = fullEntity.getString("type"),
                account = fullEntity.getString("account"),
                created = fullEntity.getLong("created"),
                json = fullEntity.getValue("json")
        )
    }

    fun add(t: StripeEvent): Key {
        val entity = FullEntity.newBuilder(keyFactory.newKey())

        entity.set("type", t.type)
        /* TODO: Investigate why this don't work as a one liner. */
        if (t.account != null)
            entity.set("account", t.account)
        else
            entity.set("account", NullValue.of())
        entity.set("created", t.created)
        entity.set("json", t.json)

        return datastore.add(entity.build()).key
    }
}
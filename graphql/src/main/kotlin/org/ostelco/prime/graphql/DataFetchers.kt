package org.ostelco.prime.graphql

import com.fasterxml.jackson.core.type.TypeReference
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.ostelco.prime.jsonmapper.objectMapper
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.ClientDataSource

val clientDataSource by lazy { getResource<ClientDataSource>() }


class SubscriberDataFetcher : DataFetcher<Map<String, Any>> {

    override fun get(env: DataFetchingEnvironment): Map<String, Any>? {
        return env.getArgument<String>("id")?.let { subscriberId ->
            val map = mutableMapOf<String, Any>()
            if (env.selectionSet.contains("profile/*")) {
                clientDataSource.getSubscriber(subscriberId)
                        .map { subscriber ->
                            map.put("profile", objectMapper.convertValue(subscriber, object : TypeReference<Map<String, Any>>() {}))
                        }
            }
            if (env.selectionSet.contains("bundles/*")) {
                clientDataSource.getBundles(subscriberId)
                        .map { bundles ->
                            map.put("bundles", bundles.map { bundle ->
                                objectMapper.convertValue<Map<String, Any>>(bundle, object : TypeReference<Map<String, Any>>() {})
                            })
                        }
            }
            if (env.selectionSet.contains("subscriptions/*")) {
                clientDataSource.getSubscriptions(subscriberId)
                        .map { subscriptions ->
                            map.put("subscriptions", subscriptions.map { subscription ->
                                objectMapper.convertValue<Map<String, Any>>(subscription, object : TypeReference<Map<String, Any>>() {})
                            })
                        }
            }
            if (env.selectionSet.contains("products/*")) {
                clientDataSource.getProducts(subscriberId)
                        .map { productsMap ->
                            map.put("products", productsMap.values.map { product ->
                                objectMapper.convertValue<Map<String, Any>>(product, object : TypeReference<Map<String, Any>>() {})
                            })
                        }
            }
            if (env.selectionSet.contains("purchases/*")) {
                clientDataSource.getPurchaseRecords(subscriberId)
                        .map { purchaseRecords ->
                            map.put("purchases", purchaseRecords.map { purchaseRecord ->
                                objectMapper.convertValue<Map<String, Any>>(purchaseRecord, object : TypeReference<Map<String, Any>>() {})
                            })
                        }
            }
            map
        }
    }
}
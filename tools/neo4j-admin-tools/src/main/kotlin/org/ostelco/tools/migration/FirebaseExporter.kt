package org.ostelco.tools.migration

import com.google.firebase.database.FirebaseDatabase
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.storage.firebase.EntityStore
import org.ostelco.prime.storage.firebase.EntityType
import org.ostelco.prime.storage.firebase.FirebaseConfig
import org.ostelco.prime.storage.firebase.FirebaseConfigRegistry

fun initFirebase() {
    FirebaseConfigRegistry.firebaseConfig = FirebaseConfig(
            configFile = "../../prime/config/pantel-prod.json",
            rootPath = "v2")
}

// Code moved here from FirebaseStorageSingleton
private val balanceEntity = EntityType("balance", Long::class.java)
private val subscriptionEntity = EntityType("subscriptions", String::class.java)
private val subscriberEntity = EntityType("subscribers", Subscriber::class.java)

// FirebaseDatabase.getInstance() will work only if FirebaseStorageSingleton.setupFirebaseInstance() is already executed.
private val balanceStore = EntityStore(FirebaseDatabase.getInstance(), balanceEntity)
private val subscriptionStore = EntityStore(FirebaseDatabase.getInstance(), subscriptionEntity)
private val subscriberStore = EntityStore(FirebaseDatabase.getInstance(), subscriberEntity)


fun importFromFirebase(action: (String) -> Unit) {
    val subscribers = subscriberStore.getAll()

    println("// Create Subscriber")
    subscribers
            .values
            .stream()
            .map { createSubscriber(it) }
            .forEach { action(it) }

    println("// Create Subscription")
    balanceStore
            .getAll()
            .keys
            .stream()
            .map { createSubscription(it) }
            .forEach { action(it) }

    println("// Add Subscription to Subscriber")
    subscriptionStore
            .getAll()
            .entries
            .stream()
            .map { addSubscriptionToSubscriber(it.key, it.value) }
            .forEach { action(it) }

    println("// Set balance")
    balanceStore
            .getAll()
            .entries
            .stream()
            .map { setBalance(it.key, it.value) }
            .forEach { action(it) }

    println("// Add subscriber to Segment")
    subscribers
            .values
            .stream()
            .map { addSubscriberToSegment(it.email) }
            .forEach { action(it) }
}
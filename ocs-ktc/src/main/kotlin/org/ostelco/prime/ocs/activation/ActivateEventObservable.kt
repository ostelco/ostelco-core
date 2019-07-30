package org.ostelco.prime.ocs.activation

import org.ostelco.prime.activation.Activation

class ActivateEventObservable : Activation by ActivateEventObservableSingleton

// TODO vihang: Maybe use ReactiveX instead of writing Observable pattern.
object ActivateEventObservableSingleton : Activation {

    private val observers = mutableSetOf<ActivateEventObserver>()

    override fun activate(msisdn: String) {
        observers.forEach { listener -> listener(msisdn) }
    }

    fun subscribe(activateEventObserver: ActivateEventObserver) = observers.add(activateEventObserver)
}

typealias ActivateEventObserver = (String) -> Unit
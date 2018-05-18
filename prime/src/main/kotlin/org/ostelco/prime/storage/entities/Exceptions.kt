package org.ostelco.prime.storage.entities

class IncompletePurchaseRequestException : Exception()

class IncompleteSubscriberException : Exception()

class NotATopupProductException(throwable: Throwable) : Throwable(throwable)
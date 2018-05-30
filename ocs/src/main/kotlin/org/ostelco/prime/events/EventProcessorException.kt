package org.ostelco.prime.events

class EventProcessorException : Exception {

    constructor(t: Throwable) : super(t)

    constructor(str: String) : super(str)

    constructor(str: String, ex: Throwable) : super(str, ex)
}

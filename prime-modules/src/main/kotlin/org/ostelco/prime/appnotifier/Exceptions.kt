package org.ostelco.prime.appnotifier

class AppNotifierException : Exception {
    constructor(t: Throwable) : super(t)

    constructor(s: String, t: Throwable) : super(s, t)

    constructor(s: String) : super(s)
}
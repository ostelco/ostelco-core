package org.ostelco.prime.storage.legacy

class StorageException : Exception {
    constructor(t: Throwable) : super(t)

    constructor(s: String, t: Throwable) : super(s, t)

    constructor(s: String) : super(s)
}
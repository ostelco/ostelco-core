package com.telenordigital.prime.storage;

public final class StorageException extends Exception {
    public StorageException(final Throwable t) {
        super(t);
    }

    public StorageException(final String s, final Throwable t) {
        super(s, t);
    }
    
    public StorageException(final String s) {
        super(s);
    }
}

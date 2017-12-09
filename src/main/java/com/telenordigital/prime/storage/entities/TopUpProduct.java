package com.telenordigital.prime.storage.entities;

public final class TopUpProduct {
    private final long noOfBytes;

    public TopUpProduct(final long noOfBytes) {
        this.noOfBytes = noOfBytes;
    }

    public long getTopUpInBytes() {
        return noOfBytes;
    }
}

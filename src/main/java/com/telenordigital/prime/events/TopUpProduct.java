package com.telenordigital.prime.events;

public final class TopUpProduct {
    private final long noOfBytes;

    public TopUpProduct(final long noOfBytes) {
        this.noOfBytes = noOfBytes;
    }

    public long getTopUpInBytes() {
        return noOfBytes;
    }
}

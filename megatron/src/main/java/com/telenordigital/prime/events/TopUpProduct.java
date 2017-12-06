package com.telenordigital.prime.events;

public final class TopUpProduct {
    private final long noOfBytes;

    public TopUpProduct(long noOfBytes) {
        this.noOfBytes = noOfBytes;
    }

    public long getTopUpInBytes() {
        return noOfBytes;
    }
}

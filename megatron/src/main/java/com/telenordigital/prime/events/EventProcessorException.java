package com.telenordigital.prime.events;

public final class EventProcessorException extends Exception {
    public EventProcessorException(final Throwable t) {
        super(t);
    }

    public EventProcessorException(String str, PurchaseRequest pr) {
        super(str);
    }
}

package com.telenordigital.prime.events;

import com.telenordigital.prime.storage.entities.PurchaseRequest;

public final class EventProcessorException extends Exception {

    private final PurchaseRequest pr;

    public EventProcessorException(final Throwable t) {
        super(t);
        this.pr = null;
    }

    public EventProcessorException(final String str, final PurchaseRequest pr) {
        super(str);
        this.pr = pr;
    }

    @Override
    public String toString() {
        return super.toString() + ", pr = " + pr.toString();
    }
}

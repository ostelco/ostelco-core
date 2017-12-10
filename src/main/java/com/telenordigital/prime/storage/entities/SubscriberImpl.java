package com.telenordigital.prime.storage.entities;

import static com.google.common.base.Preconditions.checkNotNull;


public final class SubscriberImpl implements Subscriber {

    private String fbKey;

    private String msisdn;

    private long noOfBytesLeft;

    public SubscriberImpl() {
        noOfBytesLeft = 0;
    }

    public String getFbKey() {
        return fbKey;
    }

    @Override
    public long getNoOfBytesLeft() {
        return noOfBytesLeft;
    }

    @Override
    public String getMsisdn() {
        return msisdn;
    }

    public void setFbKey(final String fbKey) {
        this.fbKey = checkNotNull(fbKey);
    }

    public void setMsisdn(final String msisdn) {
        this.msisdn = checkNotNull(msisdn);
    }

    public void setNoOfBytesLeft(final long noOfBytesLeft) {
        this.noOfBytesLeft = noOfBytesLeft;
    }

    @Override
    public String toString() {
        return "SubscriberImpl{"
                + "fbKey='" + fbKey + '\''
                + ", msisdn='" + msisdn + '\''
                + ", noOfBytesLeft=" + noOfBytesLeft
                + '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SubscriberImpl that = (SubscriberImpl) o;

        if (getNoOfBytesLeft() != that.getNoOfBytesLeft()) {
            return false;
        }
        return getFbKey().equals(that.getFbKey()) && getMsisdn().equals(that.getMsisdn());
    }

    @Override
    public int hashCode() {
        int result = getFbKey().hashCode();
        final int usefulSmallishPrime = 31;
        result *= usefulSmallishPrime;
        result += getMsisdn().hashCode();
        final int noOfBitsInAnInteger = 32;
        result *= usefulSmallishPrime;
        result += (int) (getNoOfBytesLeft() ^ (getNoOfBytesLeft() >>> noOfBitsInAnInteger));
        return result;
    }
}

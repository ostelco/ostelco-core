package com.telenordigital.prime.firebase;

import com.telenordigital.prime.events.Subscriber;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;


final class FbSubscriber implements AsMappable, Subscriber {

    private String fbKey;
    private String msisdn;

    private long noOfBytesLeft;

    FbSubscriber() {
        noOfBytesLeft = 0;
    }

    @Override
    public Map<String, Object> asMap() {
        final Map<String, Object> result = new HashMap<>();
        result.put("noOfBytesLeft", noOfBytesLeft);
        result.put("msisdn", msisdn);
        return result;
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
        this.noOfBytesLeft = checkNotNull(noOfBytesLeft);
    }


    @Override
    public String toString() {
        return "FbSubscriber{"
                + "fbKey='" + fbKey + '\''
                + ", msisdn='" + msisdn + '\''
                + ", noOfBytesLeft=" + noOfBytesLeft
                + '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        final FbSubscriber that = (FbSubscriber) o;

        if (getNoOfBytesLeft() != that.getNoOfBytesLeft()) {return false;}
        if (!getFbKey().equals(that.getFbKey())) {return false;}
        return getMsisdn().equals(that.getMsisdn());
    }

    @Override
    public int hashCode() {
        int result = getFbKey().hashCode();
        final int usefulSmallishPrime = 31;
        result = usefulSmallishPrime * result + getMsisdn().hashCode();
        final int noOfBitsInAnInteger = 32;
        result = usefulSmallishPrime * result + (int) (getNoOfBytesLeft() ^ (getNoOfBytesLeft() >>> noOfBitsInAnInteger));
        return result;
    }
}
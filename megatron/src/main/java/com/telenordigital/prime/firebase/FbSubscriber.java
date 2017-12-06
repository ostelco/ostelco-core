package com.telenordigital.prime.firebase;


import com.telenordigital.prime.events.Subscriber;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;


public final class FbSubscriber implements AsMappable, Subscriber {

    private  String fbKey;
    private  String msisdn;
    private  long noOfBytesLeft;

    public FbSubscriber() {
    }

    @Override
    public Map<String, Object> asMap() {
        final Map<String, Object>  result = new HashMap<>();
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


    public void setFbKey(String fbKey) {
        this.fbKey = checkNotNull(fbKey);
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = checkNotNull(msisdn);
    }

    public void setNoOfBytesLeft(long noOfBytesLeft) {
        this.noOfBytesLeft = checkNotNull(noOfBytesLeft);
    }


    @Override
    public String toString() {
        return "FbSubscriber{" +
                "fbKey='" + fbKey + '\'' +
                ", msisdn='" + msisdn + '\'' +
                ", noOfBytesLeft=" + noOfBytesLeft +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final FbSubscriber that = (FbSubscriber) o;

        if (getNoOfBytesLeft() != that.getNoOfBytesLeft()) return false;
        if (!getFbKey().equals(that.getFbKey())) return false;
        return getMsisdn().equals(that.getMsisdn());
    }

    @Override
    public int hashCode() {
        int result = getFbKey().hashCode();
        result = 31 * result + getMsisdn().hashCode();
        result = 31 * result + (int) (getNoOfBytesLeft() ^ (getNoOfBytesLeft() >>> 32));
        return result;
    }
}
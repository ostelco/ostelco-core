package com.telenordigital.ocsgw.diameter;

// https://tools.ietf.org/html/rfc4006#section-8.47
public class SubscriptionType {

    public static final int END_USER_E164 = 0;
    public static final int END_USER_IMSI = 1;
    public static final int END_USER_SIP_URI = 2;
    public static final int END_USER_NAI = 3;
    public static final int END_USER_PRIVATE = 4;
}

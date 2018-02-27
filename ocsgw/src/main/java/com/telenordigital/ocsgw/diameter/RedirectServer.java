package com.telenordigital.ocsgw.diameter;


enum RedirectAddressType {
    IPV4_ADDRESS,
    IPV6_ADDRESS,
    URL,
    SIP_URL
}


public class RedirectServer {
    private RedirectAddressType redirectAddressType;
    String redirectServerAddress;
}

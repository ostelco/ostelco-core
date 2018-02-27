package com.telenordigital.ocsgw.diameter;

enum Action {
    PERMIT,
    DENY
}

enum Direction {
    IN,
    OUT
}


public class IPFilterRule {
    Action action;
    Direction direction;
    String proto;
    String host;
}

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
    private Action action;
    private Direction direction;
    private String proto;
    private String host;
}

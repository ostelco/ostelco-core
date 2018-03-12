package com.telenordigital.ostelco.diameter.parser

interface Parser<T> {
    fun parse(): T
}
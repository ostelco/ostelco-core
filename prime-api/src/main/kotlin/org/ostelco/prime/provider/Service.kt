package org.ostelco.prime.provider

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import io.dropwizard.jackson.Discoverable
import io.dropwizard.setup.Environment

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
interface Service : Discoverable {
    fun init(env: Environment)
}
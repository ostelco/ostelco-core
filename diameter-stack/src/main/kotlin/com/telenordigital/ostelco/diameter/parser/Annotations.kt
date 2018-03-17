package com.telenordigital.ostelco.diameter.parser

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.reflect.KClass

@Target(FIELD)
@Retention(RUNTIME)
annotation class AvpField(val avpId: Int)

@Target(FIELD)
@Retention(RUNTIME)
annotation class AvpGroup(
        val avpId: Int,
        val kclass: KClass<*>)
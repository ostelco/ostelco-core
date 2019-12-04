package org.ostelco.diameter.parser

import org.jdiameter.api.Avp
import org.jdiameter.api.AvpSet
import org.ostelco.diameter.getLogger
import org.ostelco.diameter.util.AvpType
import org.ostelco.diameter.util.AvpType.*
import org.ostelco.diameter.util.AvpTypeDictionary
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberProperties

class AvpParser {

    private val logger by getLogger()

    /**
     * @param kclazz Kotlin class representing the data type of the AVP set getting parsed.
     * @param avpSet Set of AVPs which
     */
    fun <T:Any> parse(kclazz: KClass<T>, avpSet: AvpSet): T {

        // Need java and Kotlin class, both.
        val clazz = kclazz.java

        // The map which will store the values
        val map: MutableMap<String, Any> = HashMap()

        // Create an object using no-arg primary constructor
        val instance = kclazz.createInstance()

        // For some reason, the Kotlin reflection is not able to fetch the annotation fields (AvpID in our case).
        // So, using Java Reflection to do same.
        // But once the object field value is ready, it cannot be set to field using Java Reflection,
        // since these are Kotlin classes.

        // So, there are 2 loops here. First using Java reflection to fetch Annotation field values.
        // And 2nd loop using Kotlin reflection to set object field values.

        // loop over all the fields in that class
        for (field in clazz.declaredFields) {

            if (field.isAnnotationPresent(AvpField::class.java)) {

                // Get numeric Avp ID from annotation on the field
                val avpId: Int? = field.getAnnotation(AvpField::class.java)?.avpId
                logger.trace("${field.name} id: ($avpId)")

                if (avpId != null) {

                    // Get Avp Object from the Set.
                    // Avp object has AvpCode, Vendor ID, and a value which will be set on object field.
                    val avp: Avp? = avpSet.getAvp(avpId)

                    val avpValue = getAvpValue(field, avp)

                    // finally, the value is saved in Map.
                    // This map is then used in the 2nd loop, where the value is "set" on object field using
                    // Kotlin reflection
                    if (avpValue != null) {
                        logger.trace("${field.name} will be set to $avpValue")
                        map[field.name] = avpValue
                    }
                }

            } else if (field.isAnnotationPresent(AvpList::class.java)) {

                // Get numeric Avp ID from annotation on the field
                val avpId: Int? = field.getAnnotation(AvpList::class.java)?.avpId
                logger.trace("${field.name} id: ($avpId)")

                if (avpId != null) {

                    // Get Avp Object from the Set.
                    // Avp object has AvpCode, Vendor ID, and a value which will be set on object field.
                    val avpFieldSet: AvpSet? = avpSet.getAvps(avpId)
                    val avpListValue = mutableListOf<Any>()
                    if (avpFieldSet != null) {
                        for (avp in avpFieldSet) {
                            getAvpValue(field, avp)?.also {
                                avpListValue.add(it)
                            }
                        }
                    }

                    // finally, the value is saved in Map.
                    // This map is then used in the 2nd loop, where the value is "set" on object field using
                    // Kotlin reflection
                    if (avpListValue.isNotEmpty()) {
                        logger.trace("${field.name} will be set to $avpListValue")
                        map[field.name] = avpListValue
                    }
                }
            }
        }
        // Now, the values to be set in object field are ready in the map.
        // Iterating over fields again, but using Kotlin reflection this time.
        kclazz.declaredMemberProperties
                // filter out fields which are not present in the map.
                .filter { map.containsKey(it.name) }
                .forEach {
                    if (it is KMutableProperty<*>) {
                        val avpValue = map.getValue(it.name)
                        logger.trace("${it.name} set to $avpValue")
                        try {
                            // If the field is of type list, then merge the existing values with new value from the map.
                            // The set the merged list back.
                            if (avpValue is Collection<*>) {
                                // get the existing list field values
                                val list = it.getter.call(instance) as Collection<*>?
                                // create new list for merging
                                val mergedList = ArrayList<Any?>()
                                if (list != null) {
                                    // add existing field list
                                    mergedList.addAll(list)
                                }
                                // add new value to merge list
                                mergedList.addAll(avpValue)
                                // call setter to set the merged list back
                                it.setter.call(instance, mergedList)
                            } else {
                                // For simple case, call setter of the object field
                                it.setter.call(instance, avpValue)
                            }
                        } catch (e: Exception) {
                            logger.error("Failed to set $avpValue to ${it.name} for ${kclazz.simpleName}", e)
                        }
                    }
                }
        return instance
    }

    private fun getAvpValue(field: Field, avp: Avp?): Any? {

        // Check the data type of the field
        val collectionType: KClass<*>? = field.getAnnotation(AvpList::class.java)?.kclass

        if (avp != null) {

            logger.trace("${field.name} has type ${field.type}")

            return when {
                // if the target class is Avp itself, the avp object itself is target value
                field.type.kotlin == Avp::class -> avp
                // The field is of type List. So, even the Avp Value is saved in a list.
                // Even though this list has a single value, it helps in distinguishing while setting
                // the value back.
                field.type.kotlin == List::class -> {
                    if (avp.grouped != null && collectionType != null) {
                        val avpValue = parse(collectionType, avp.grouped)
                        logger.trace("To list of ${collectionType.simpleName} adding: $avpValue")
                        avpValue
                    } else {
                        null
                    }
                }
                field.type.isEnum -> {
                    // Fetch int value to be mapped to enum
                    val intEnum = getAvpValue(field.type.kotlin, avp) as Int

                    // Array of enum values for the given enum type of the field
                    val enumArray = field.type.enumConstants

                    try {
                        // using try block, check if the Enum class has 'value' property
                        val valueField = field.type.getDeclaredField("value")
                        enumArray.first { valueField.getInt(field) == intEnum }
                    } catch (e: Exception) {
                        // int value is ordinal of enum. So, directly using the enum const array
                        enumArray[intEnum]
                    }
                }
                else -> {
                    logger.trace("Field: ${field.name}")
                    // for simple case, fetch target value for given Avp
                    getAvpValue(field.type.kotlin, avp)
                }
            }
        }
        return null
    }

    private fun getAvpValue(kclazz: KClass<*>, avp: Avp): Any? {

        val type = AvpTypeDictionary.getType(avp)

        logger.trace("Type: $type")

        if (type == null) {
            logger.error("Unknown type: $type for avpCode: ${avp.code}")
            return avp.utF8String
        }
        return when (type) {
            ADDRESS -> avp.address
            DIAMETER_IDENTITY -> avp.diameterIdentity
            DIAMETER_URI -> avp.diameterURI
            FLOAT32 -> avp.float32
            FLOAT64 -> avp.float64
            ENUMERATED -> avp.integer32
            GROUPED -> parse(kclazz, avp.grouped)
            INTEGER32, APP_ID -> avp.integer32
            INTEGER64 -> avp.integer64
            IP_ADDRESS -> avp.address
            IP_FILTER_RULE -> avp.octetString
            OCTET_STRING -> avp.octetString
            QOS_FILTER_RULE -> avp.octetString
            RAW -> avp.raw
            RAW_DATA -> avp.rawData
            TIME -> avp.time
            UNSIGNED32, VENDOR_ID -> avp.unsigned32
            UNSIGNED64 -> avp.unsigned64
            UTF8STRING -> avp.utF8String
        }
    }
}

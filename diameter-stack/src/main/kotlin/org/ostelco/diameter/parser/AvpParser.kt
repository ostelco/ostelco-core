package org.ostelco.diameter.parser

import org.ostelco.diameter.logger
import org.jdiameter.api.Avp
import org.jdiameter.api.AvpSet
import org.mobicents.diameter.dictionary.AvpDictionary
import org.mobicents.diameter.dictionary.AvpRepresentation
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberProperties

class AvpParser {

    private val LOG by logger()

    init {
        AvpDictionary.INSTANCE.parseDictionary("config/dictionary.xml")
    }

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
        clazz.declaredFields
                // filter out fields which are not annotated
                .filter {
                    it.isAnnotationPresent(AvpField::class.java)
                            || it.isAnnotationPresent(AvpGroup::class.java)
                }
                .forEach {
                    // Get numeric Avp ID from annotation on the field
                    val avpId: Int? = it.getAnnotation(AvpField::class.java)?.avpId
                            ?: it.getAnnotation(AvpGroup::class.java)?.avpId

                    LOG.trace("${it.name} id: ($avpId)")
                    if (avpId != null) {

                        // Check the data type of the field
                        val collectionType: KClass<*>? = it.getAnnotation(AvpGroup::class.java)?.kclass

                        // Get Avp Object from the Set.
                        // Avp object has AvpCode, Vendor ID, and a value which will be set on object field.
                        val avp: Avp? = avpSet.getAvp(avpId)

                        if (avp != null) {

                            LOG.trace("${it.name} has type ${it.type}")

                            val avpValue = when {
                                // if the target class is Avp itself, the avp object itself is target value
                                it.type.kotlin == Avp::class -> avp
                                // The field is of type List. So, even the Avp Value is saved in a list.
                                // Even though this list has a single value, it helps in distinguishing while setting
                                // the value back.
                                it.type.kotlin == List::class -> {
                                    val list = ArrayList<Any?>()
                                    if (avp.grouped != null && collectionType != null) {
                                        val avpValue = parse(collectionType, avp.grouped)
                                        LOG.trace("To list of ${collectionType.simpleName} adding: $avpValue")
                                        list.add(avpValue)
                                    }
                                    list
                                }
                                it.type.isEnum -> {
                                    // Fetch int value to be mapped to enum
                                    val intEnum = getAvpValue(it.type.kotlin, avp) as Int

                                    // Array of enum values for the given enum type of the field
                                    val enumArray = it.type.enumConstants

                                    try {
                                        // using try block, check if the Enum class has 'value' property
                                        val valueField = it.type.getDeclaredField("value")
                                         enumArray.filter { valueField.getInt(it) == intEnum }.first()
                                    } catch (e : Exception) {
                                        // int value is ordinal of enum. So, directly using the enum const array
                                        enumArray[intEnum]
                                    }
                                }
                                else ->  {
                                    LOG.trace("Field: ${it.name}")
                                    // for simple case, fetch target value for given Avp
                                    getAvpValue(it.type.kotlin, avp)
                                }
                            }
                            // finally, the value is saved in Map.
                            // This map is then used in the 2nd loop, where the value is "set" on object field using
                            // Kotlin reflection
                            if (avpValue != null) {
                                LOG.trace("${it.name} will be set to $avpValue")
                                map.put(it.name, avpValue)
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
                        LOG.trace("${it.name} set to $avpValue")
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
                            LOG.error("Failed to set $avpValue to ${it.name} for ${kclazz.simpleName}", e)
                        }
                    }
                }
        return instance;
    }

    private fun getAvpValue(kclazz: KClass<*>, avp: Avp): Any? {

        // We need to know the data type of the given AVP so that we call right method to fetch the value.
        // Metadata about AVP is lookup into a dictionary.
        var avpRep: AvpRepresentation? = AvpDictionary.INSTANCE.getAvp(avp.code, avp.vendorId)
        // If the lookup returns null,
        if (avpRep == null) {
            avpRep = AvpDictionary.INSTANCE.getAvp(avp.code)
        }
        if (avpRep == null) {
            LOG.error("AVP ${avp.code} missing in dictionary")
            return null
        }

        val type = avpRep.type

        LOG.trace("Type: $type")
        return when (type) {
            "Address" -> avp.address
            "Identity" -> avp.diameterIdentity
            "URI" -> avp.diameterURI
            "Float32" -> avp.float32
            "Float64" -> avp.float64
            "Grouped" -> parse(kclazz, avp.grouped)
            "Integer32" -> avp.integer32
            "Integer64" -> avp.integer64
            "OctetString" -> avp.octetString
            "Raw" -> avp.raw
            "RawData" -> avp.rawData
            "Time" -> avp.time
            "Unsigned32" -> avp.unsigned32
            "Unsigned64" -> avp.unsigned64
            "UTF8String" -> avp.utF8String
            else -> {
                LOG.error("Unknown type: $type for avpCode: ${avp.code}")
                avp.utF8String
            }
        }
    }
}

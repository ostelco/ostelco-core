package org.ostelco.prime.storage.graph

import kotlin.test.Test
import kotlin.test.assertEquals

class ObjectHandlerTest {

    private val separator = "/"

    @Test
    fun `test object to map and back`() {
        val map = ObjectHandler.getProperties(createProduct("1GB_249NOK"))

        val expectedMap = LinkedHashMap<String, Any>()
        expectedMap["sku"] = "1GB_249NOK"
        expectedMap["price${separator}amount"] = 24900
        expectedMap["price${separator}currency"] = "NOK"
        expectedMap["properties${separator}noOfBytes"] = "1_073_741_824"
        expectedMap["properties${separator}productClass"] = "SIMPLE_DATA"
        expectedMap["presentation${separator}label"] = "1 GB for 249"

        assertEquals(expectedMap, map)

        val expectedNestedMap = LinkedHashMap<String, Any>()
        expectedNestedMap["sku"] = "1GB_249NOK"
        val priceMap = LinkedHashMap<String, Any>()
        expectedNestedMap["price"] = priceMap
        priceMap["amount"] = 24900
        priceMap["currency"] = "NOK"
        val propertiesMap = LinkedHashMap<String, Any>()
        expectedNestedMap["properties"] = propertiesMap
        propertiesMap["noOfBytes"] = "1_073_741_824"
        propertiesMap["productClass"] = "SIMPLE_DATA"
        val presentationMap = LinkedHashMap<String, Any>()
        expectedNestedMap["presentation"] = presentationMap
        presentationMap["label"] = "1 GB for 249"

        val nestedMap = ObjectHandler.toNestedMap(map)
        assertEquals(expectedNestedMap, nestedMap)
    }
}
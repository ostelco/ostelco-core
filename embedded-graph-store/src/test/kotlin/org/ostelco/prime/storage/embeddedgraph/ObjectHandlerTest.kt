package org.ostelco.prime.storage.embeddedgraph

import kotlin.test.Test
import kotlin.test.assertEquals

class ObjectHandlerTest {

    @Test
    fun `test object to map and back`() {
        val map = ObjectHandler.getProperties(createProduct("1GB_249NOK", 24900))

        val expectedMap = LinkedHashMap<String, Any>()
        expectedMap["sku"] = "1GB_249NOK"
        expectedMap["price/amount"] = 24900
        expectedMap["price/currency"]  = "NOK"
        expectedMap["properties/noOfBytes"]  = "1073741824"
        expectedMap["presentation/label"]  = "1 GB for 249"

        assertEquals(expectedMap, map)

        val expectedNestedMap = LinkedHashMap<String, Any>()
        expectedNestedMap["sku"] = "1GB_249NOK"
        val priceMap = LinkedHashMap<String, Any>()
        expectedNestedMap["price"] = priceMap
        priceMap["amount"]  = 24900
        priceMap["currency"]  = "NOK"
        val propertiesMap = LinkedHashMap<String, Any>()
        expectedNestedMap["properties"] = propertiesMap
        propertiesMap["noOfBytes"]  = "1073741824"
        val presentationMap = LinkedHashMap<String, Any>()
        expectedNestedMap["presentation"] = presentationMap
        presentationMap["label"]  = "1 GB for 249"

        val nestedMap = ObjectHandler.toNestedMap(map)
        assertEquals(expectedNestedMap, nestedMap)
    }
}
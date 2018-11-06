package org.ostelco

import com.github.fge.jsonschema.core.load.SchemaLoader
import junit.framework.Assert.assertNotNull
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Test

class TestJsonValidation() {

    @Test
    fun helloWorldTest() {
        val inputStream = this.javaClass.getResourceAsStream("/hello-world-schema.json")
        assertNotNull(inputStream)
        val rawSchema = JSONObject(JSONTokener(inputStream))
        val schema =  org.everit.json.schema.loader.SchemaLoader.load(rawSchema)
        schema.validate(JSONObject("{\"hello\" : \"world\"}"))
    }



    @Test
    fun ES2DownloadOrderTest() {
        val inputStream = this.javaClass.getResourceAsStream("/es2schemas/ES2+DownloadOrder-def.json")
        assertNotNull(inputStream)
        val rawSchema = JSONObject(JSONTokener(inputStream))
        val schema =  org.everit.json.schema.loader.SchemaLoader.load(rawSchema)
        schema.validate(JSONObject( "{\"eid\" : \"01234567890123456789012345678901\", \"iccid\" : \"01234567890123456789\", \"profileType\" : \"Eplestang\"}"))
    }
}
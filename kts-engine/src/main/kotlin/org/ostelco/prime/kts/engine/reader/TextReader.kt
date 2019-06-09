package org.ostelco.prime.kts.engine.reader

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.jackson.Discoverable

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
interface TextReader : Discoverable {
    fun readText(): String
}

@JsonTypeName("classpathResource")
data class ClasspathResourceTextReader(val filename: String) : TextReader {
    override fun readText(): String = {}.javaClass.getResource(filename).readText()
}

@JsonTypeName("file")
data class FileTextReader(val filename: String) : TextReader {
    override fun readText() = java.io.FileReader(filename).readText()
}

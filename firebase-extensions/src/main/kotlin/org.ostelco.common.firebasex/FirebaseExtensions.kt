package org.ostelco.common.firebasex

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseOptions.Builder
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import javax.naming.ConfigurationException

private val genericMapType = object : TypeReference<Map<String, String>>() {}
private val objectMapper = ObjectMapper().registerKotlinModule()

/**
 * Extension function added into {@link com.google.firebase.FirebaseOptions.Builder} which accepts Firebase Credentials
 * file and sets credentials as well as database URL. Database URL needs database name, which is extracted from the same
 * credentials file. If the credentials file does not exists, it throws ConfigurationException.
 */
fun Builder.usingCredentialsFile(credentialsFile: String): Builder {
    if (Files.exists(Paths.get(credentialsFile))) {
        val credentials: GoogleCredentials = FileInputStream(credentialsFile)
                .use { serviceAccount -> GoogleCredentials.fromStream(serviceAccount) }
        val map: Map<String, String> = objectMapper.readValue(File(credentialsFile), genericMapType)
        val databaseName = map["project_id"]
        setCredentials(credentials)
        setDatabaseUrl("https://$databaseName.firebaseio.com/")
        return this
    }
    throw ConfigurationException("Missing Firebase Credentials file: $credentialsFile")
}
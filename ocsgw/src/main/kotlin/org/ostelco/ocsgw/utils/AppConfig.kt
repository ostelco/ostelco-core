package org.ostelco.ocsgw.utils

import org.ostelco.diameter.getLogger
import org.ostelco.ocsgw.datasource.DataSourceType
import org.ostelco.ocsgw.datasource.SecondaryDataSourceType
import java.util.*

class AppConfig {

    private val logger by getLogger()

    private val prop = Properties()
    // OCS_DATASOURCE_TYPE env has higher preference over config.properties
    val dataStoreType: DataSourceType
        get() { // OCS_DATASOURCE_TYPE env has higher preference over config.properties
            val dataSource = System.getenv("OCS_DATASOURCE_TYPE")
            return if (dataSource.isNullOrEmpty()) {
                try {
                    DataSourceType.valueOf(prop.getProperty("DataStoreType", "Local"))
                } catch (e: IllegalArgumentException) {
                    logger.warn("Could not get the DataSourceType from property")
                    DataSourceType.Local
                }
            } else try {
                DataSourceType.valueOf(dataSource)
            } catch (e: IllegalArgumentException) {
                logger.warn("Could not get the DataSourceType from env")
                DataSourceType.Local
            }
        }

    val defaultRequestedServiceUnit: Long
        get() {
            val defaultRequestedServiceUnit = System.getProperty("DEFAULT_REQUESTED_SERVICE_UNIT")
            return if (defaultRequestedServiceUnit.isNullOrEmpty()) {
                40000000L
            } else {
                defaultRequestedServiceUnit.toLong()
            }
        }

    // OCS_SECONDARY_DATASOURCE_TYPE env has higher preference over config.properties
    val secondaryDataStoreType: SecondaryDataSourceType
        get() { // OCS_SECONDARY_DATASOURCE_TYPE env has higher preference over config.properties
            val secondaryDataSource = System.getenv("OCS_SECONDARY_DATASOURCE_TYPE")
            return if (secondaryDataSource.isNullOrEmpty()) {
                try {
                    SecondaryDataSourceType.valueOf(prop.getProperty("SecondaryDataStoreType", "PubSub"))
                } catch (e: IllegalArgumentException) {
                    SecondaryDataSourceType.PubSub
                }
            } else try {
                SecondaryDataSourceType.valueOf(secondaryDataSource)
            } catch (e: IllegalArgumentException) {
                logger.warn("Could not get the Secondary DataSourceType")
                SecondaryDataSourceType.PubSub
            }
        }

    val grpcServer: String
        get() = getEnvProperty("OCS_GRPC_SERVER")

    val pubSubProjectId: String
        get() = getEnvProperty("PUBSUB_PROJECT_ID")

    val pubSubTopicIdForCcr: String
        get() = getEnvProperty("PUBSUB_CCR_TOPIC_ID")

    val pubSubTopicIdForCca: String
        get() = getEnvProperty("PUBSUB_CCA_TOPIC_ID")

    val pubSubSubscriptionIdForCca: String
        get() = getEnvProperty("PUBSUB_CCA_SUBSCRIPTION_ID")

    val pubSubSubscriptionIdForActivate: String
        get() = getEnvProperty("PUBSUB_ACTIVATE_SUBSCRIPTION_ID")

    private fun getEnvProperty(propertyName: String): String {
        val value = System.getenv(propertyName)
        if (value.isNullOrEmpty()) {
            throw Error("No $propertyName set in env")
        }
        return value
    }

    init {
        val fileName = "config.properties"
        val iStream = this.javaClass.classLoader.getResourceAsStream(fileName)
        prop.load(iStream)
        iStream?.close()
    }
}
package org.ostelco.prime.imei.core

data class Imei(val tac: String,
                val marketingName: String,
                val manufacturer: String,
                val brandName: String,
                val modelName: String,
                val operatingSystem: String,
                val deviceType: String,
                val oem: String)
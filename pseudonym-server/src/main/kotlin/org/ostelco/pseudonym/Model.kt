package org.ostelco.pseudonym

open class Kind(val kindName: String, val idPropertyName: String)

class PseudonymKind(kindName: String, idPropertyName: String) : Kind(kindName = kindName, idPropertyName = idPropertyName) {
    val pseudonymPropertyName = "pseudonym"
    val startPropertyName = "start"
    val endPropertyName = "end"
}

enum class PseudonymKindEnum(val kindInfo: PseudonymKind) {
    MSISDN(PseudonymKind(kindName = "Pseudonym", idPropertyName = "msisdn")),
    SUBSCRIBER_ID(PseudonymKind(kindName = "SubscriberPseudonym", idPropertyName = "subscriberId"))
}

object ExportTaskKind : Kind (kindName = "ExportTask", idPropertyName = "exportId") {
    const val statusPropertyName = "status"
    const val errorPropertyName = "error"
}
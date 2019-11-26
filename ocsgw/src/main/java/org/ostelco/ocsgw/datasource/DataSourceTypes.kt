package org.ostelco.ocsgw.datasource

enum class DataSourceType {
    Local,
    gRPC,
    PubSub,
    Proxy,
    Multi
}

enum class DataSourceOperations {
    activate,
    creditControl,
    creditControlAndActivate
}
package org.ostelco.ocsgw.datasource

enum class DataSourceType {
    Local,
    gRPC,
    PubSub,
    Proxy,
    Multi
}

enum class SecondaryDataSourceType {
    gRPC,
    PubSub
}
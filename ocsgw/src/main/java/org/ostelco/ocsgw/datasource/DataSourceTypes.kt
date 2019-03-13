package org.ostelco.ocsgw.datasource

enum class DataSourceType {
    Local,
    gRPC,
    PubSub,
    Proxy
}

enum class SecondaryDataSourceType {
    gRPC,
    PubSub
}
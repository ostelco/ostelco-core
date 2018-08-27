package org.ostelco.bqmetrics.backend;

import org.skife.jdbi.v2.sqlobject.SqlQuery;

public abstract class DatabaseBackend {
    @SqlQuery("select 1")
    public abstract Integer exampleQuery();
}

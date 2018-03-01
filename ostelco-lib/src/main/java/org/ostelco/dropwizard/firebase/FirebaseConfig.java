package org.ostelco.dropwizard.firebase;

public final class FirebaseConfig  {
    @NotEmpty
    @JsonProperty("databaseName")
    private String databaseName;

    @NotEmpty
    @JsonProperty("configFile")
    private String configFile;

    public String getDatabaseName() {
        return databaseName;
    }

    public String getConfigFile() {
        return configFile;
    }
}

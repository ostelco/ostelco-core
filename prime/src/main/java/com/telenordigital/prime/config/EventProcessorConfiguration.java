package com.telenordigital.prime.config;

public final class EventProcessorConfiguration {

    @NotEmpty
    @JsonProperty("configFile")
    private String configFile;

    @NotEmpty
    @JsonProperty("projectId")
    private String projectId;

    @NotEmpty
    @JsonProperty("topicId")
    private String topicId;

    public String getConfigFile() {
        return configFile;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getTopicId() {
        return topicId;
    }
}

package org.ostelco.topup.api.core;

public class Error {

    private final String description;

    public Error(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

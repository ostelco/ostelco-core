package org.ostelco.topup.api.core;

import lombok.Data;
import lombok.NonNull;

@Data
public class Error {
    @NonNull final private String description;
}

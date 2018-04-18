package org.ostelco.topup.api.core;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Profile {
    private final String name;
    private final String email;
}

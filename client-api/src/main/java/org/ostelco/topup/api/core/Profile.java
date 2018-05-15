package org.ostelco.topup.api.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class Profile {
    private final String name;
    private final String email;

    @JsonIgnore
    public boolean isValid() {
        return name != null && !name.isEmpty() && email != null && !email.isEmpty();
    }
}

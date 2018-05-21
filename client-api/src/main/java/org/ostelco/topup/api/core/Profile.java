package org.ostelco.topup.api.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class Profile {
    private String name;
    private String address;
    private String postCode;
    private String city;
    private final String email;

    /**
     * Minimum is that 'name' and 'email' is present.
     */
    @JsonIgnore
    public boolean isValid() {
        return name != null && !name.isEmpty() && email != null && !email.isEmpty();
    }
}

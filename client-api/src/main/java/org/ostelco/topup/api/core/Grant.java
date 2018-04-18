package org.ostelco.topup.api.core;

import lombok.Data;

@Data
public class Grant {
    private String grantType;
    private String code;
    private String refreshToken;
}

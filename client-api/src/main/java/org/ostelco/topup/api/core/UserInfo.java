package org.ostelco.topup.api.core;

import lombok.Data;

/**
 * Captures the user info data (consented claims) fetched from the OAuth2
 * service provider, when calling the 'https://[base-server-url]/userinfo'
 * endpoint.
 *
 * Ref.: http://openid.net/specs/openid-connect-core-1_0.html#UserInfo
 *       https://auth0.com/docs/api/authentication#get-user-info
 */
@Data
public class UserInfo {
    final private boolean emailVerified;
    final private String email;
    final private String updatedAt;
    final private String name;
    final private String picture;       /* And URL. */
    final private String userId;        /* An OpenID id.*/
    final private String nickname;
    final private String createdAt;
    final private String sub;           /* An OpenID id. */
}

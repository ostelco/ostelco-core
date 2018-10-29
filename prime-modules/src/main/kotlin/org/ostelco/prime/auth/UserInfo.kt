package org.ostelco.prime.auth

/**
 * Captures the user info data (consented claims) fetched from the OAuth2
 * service provider, when calling the 'https://[base-server-url]/userinfo'
 * endpoint.
 *
 * Ref.: http://openid.net/specs/openid-connect-core-1_0.html#UserInfo
 * https://auth0.com/docs/api/authentication#get-user-info
 */
class UserInfo {
    var isEmailVerified: Boolean = false
    var email: String? = null
    var updatedAt: String? = null
    var name: String? = null
    var picture: String? = null       /* And URL. */
    var userId: String? = null        /* An OpenID id.*/
    var nickname: String? = null
    var createdAt: String? = null
    var sub: String? = null           /* An OpenID id. */
}

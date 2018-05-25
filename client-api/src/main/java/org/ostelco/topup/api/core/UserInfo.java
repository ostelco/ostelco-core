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
    private boolean emailVerified;
    private String email;
    private String updatedAt;
    private String name;
    private String picture;       /* And URL. */
    private String userId;        /* An OpenID id.*/
    private String nickname;
    private String createdAt;
    private String sub;           /* An OpenID id. */

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }
}

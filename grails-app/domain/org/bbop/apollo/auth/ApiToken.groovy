package org.bbop.apollo.auth

import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.AuthenticationToken
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class ApiToken implements AuthenticationToken {

    // username
    final String username
    private final String apikey

    ApiToken(String apikey) {
        this.apikey = apikey
        this.username = SecurityUtils.subject.principal
    }

    private ApiToken(String username, String apikey) {
        this.username = username
        this.apikey = apikey
    }
    
    static ApiToken fromApiTokenString(String apiTokenString) {
        List<String> tokens = apiTokenString?.tokenize(':')
        if (tokens.size() != 2) {
            throw new IllegalArgumentException('Invalid token format')
        }
        String username = tokens[0]
        String apikey = tokens[1]
        new ApiToken(username, apikey)
    }
    
    String getTokenString() {
        username + ':' + apikey
    }

    
    boolean equals(Object obj) {
        if (!(obj instanceof ApiToken)) {
            return false
        }
        ApiToken other = (ApiToken) obj
        Objects.equal(username, other.username) && Objects.equal(apikey, other.apikey)
    }

    int hashCode() {
        Objects.hashCode(username, apikey)
    }

    /*
     * 
     * @see org.apache.shiro.authc.AuthenticationToken#getCredentials()
     */
    Object getCredentials() {
        getTokenString()
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.shiro.authc.AuthenticationToken#getPrincipal()
     */
    Object getPrincipal() {
        username
    }

}
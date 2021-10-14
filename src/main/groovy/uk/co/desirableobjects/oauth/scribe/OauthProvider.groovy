package uk.co.desirableobjects.oauth.scribe

import com.github.scribejava.core.oauth.OAuthService

class OauthProvider {
    String successUri
    String failureUri
    OAuthService service

    SupportedOauthVersion getOauthVersion() {
        return SupportedOauthVersion.parse(service.version)
    }
}
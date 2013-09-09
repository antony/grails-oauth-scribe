package uk.co.desirableobjects.oauth.scribe

import org.scribe.oauth.OAuthService

class OauthProvider {

    String successUri
    String failureUri
	String callback

    OAuthService service

    SupportedOauthVersion getOauthVersion() {
        return SupportedOauthVersion.parse(service.version)
    }

}

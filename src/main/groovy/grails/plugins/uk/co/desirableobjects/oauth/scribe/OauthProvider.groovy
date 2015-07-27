package grails.plugins.uk.co.desirableobjects.oauth.scribe

import org.scribe.oauth.OAuthService

class OauthProvider {

    String successUri
    String failureUri

    OAuthService service

    SupportedOauthVersion getOauthVersion() {
        return SupportedOauthVersion.parse(service.version)
    }

}

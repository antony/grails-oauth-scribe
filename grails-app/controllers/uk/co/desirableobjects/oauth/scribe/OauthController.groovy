package uk.co.desirableobjects.oauth.scribe

import org.scribe.model.Verifier
import org.scribe.model.Token
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

class OauthController {

    private final Token EMPTY_TOKEN = null

    OauthService oauthService

    def callback = {

        String providerName = params.provider
        OauthProvider provider = oauthService.findProviderConfiguration(providerName)

        Verifier verifier = extractVerifier(provider, params)

        if (!verifier) {
            return redirect(uri: provider.failureUri)
        }

        Token requestToken = (Token) session[oauthService.findSessionKeyForRequestToken(providerName)]
        Token accessToken = oauthService.getAccessToken(providerName, requestToken, verifier)

        session[oauthService.findSessionKeyForAccessToken(providerName)] = accessToken
        session.removeAttribute(oauthService.findSessionKeyForRequestToken(providerName))

        return redirect(uri: provider.successUri)

    }

    private Verifier extractVerifier(OauthProvider provider, GrailsParameterMap params) {
        
        String verifierKey = 'oauth_verifier'
        if (SupportedOauthVersion.TWO == provider.oauthVersion) {
            verifierKey = 'code'
        }

        if (!params[verifierKey]) {
             log.error("Cannot authenticate with oauth: Could not find oauth verifier in ${params}.")
             return null
        }

        String verification = params[verifierKey]
        return new Verifier(verification)

    }

    def authenticate = {

        String providerName = params.provider
        OauthProvider provider = oauthService.findProviderConfiguration(providerName)

        Token requestToken = EMPTY_TOKEN
        if (provider.getOauthVersion() == SupportedOauthVersion.ONE) {
            requestToken = provider.service.requestToken
        }

        session[oauthService.findSessionKeyForRequestToken(providerName)] = requestToken
        String url = oauthService.getAuthorizationUrl(providerName, requestToken)
        
        return redirect(url: url)
    }

}

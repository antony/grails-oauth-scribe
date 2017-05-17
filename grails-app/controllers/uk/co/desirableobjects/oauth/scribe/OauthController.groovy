package uk.co.desirableobjects.oauth.scribe

import com.github.scribejava.core.exceptions.OAuthException
import com.github.scribejava.core.model.OAuth2AccessToken
import com.github.scribejava.core.model.Token
import grails.web.servlet.mvc.GrailsParameterMap
import uk.co.desirableobjects.oauth.scribe.exception.MissingRequestTokenException
import uk.co.desirableobjects.oauth.scribe.holder.RedirectHolder

class OauthController {
    private static final Token EMPTY_TOKEN = new OAuth2AccessToken('', '')

    OauthService oauthService

    def callback() {

        String providerName = params.provider
        OauthProvider provider = oauthService.findProviderConfiguration(providerName)

        String verifier = extractVerifier(provider, params)

        if (!verifier) {
            redirect(uri: provider.failureUri)
            return
        }

        Token requestToken = provider.oauthVersion == SupportedOauthVersion.TWO ?
            EMPTY_TOKEN :
            (Token) session[oauthService.findSessionKeyForRequestToken(providerName)]

        if (!requestToken) {
            throw new MissingRequestTokenException(providerName)
        }

        Token accessToken

        try {
            accessToken = provider.oauthVersion == SupportedOauthVersion.ONE ?
              oauthService.getAccessTokenForOAuth1(providerName, requestToken, verifier) :
              oauthService.getAccessTokenForOAuth2(providerName, params?.code)
        } catch(OAuthException ex){
            log.error("Cannot authenticate with oauth: ${ex.toString()}")
            return redirect(uri: provider.failureUri)
        }

        session[oauthService.findSessionKeyForAccessToken(providerName)] = accessToken
        session.removeAttribute(oauthService.findSessionKeyForRequestToken(providerName))

        return redirect(uri: provider.successUri)

    }

    private String extractVerifier(OauthProvider provider, GrailsParameterMap params) {

        String verifierKey = determineVerifierKey(provider)

        if (!params[verifierKey]) {
            log.error("Cannot authenticate with oauth: Could not find oauth verifier in ${params}.")
            return null
        }

        return params[verifierKey]

    }

    private String determineVerifierKey(OauthProvider provider) {

        return SupportedOauthVersion.TWO == provider.oauthVersion ? 'code' : 'oauth_verifier'

    }

    def authenticate() {

        String providerName = params.provider
        OauthProvider provider = oauthService.findProviderConfiguration(providerName)

        Token requestToken = EMPTY_TOKEN
        String url
        if (provider.oauthVersion == SupportedOauthVersion.ONE) {
            requestToken = provider.service.requestToken
            url = oauthService.getAuthorizationUrlForOAuth1(providerName, requestToken)
        } else {
            url = oauthService.getAuthorizationUrlForOAuth2(providerName)
        }

        session[oauthService.findSessionKeyForRequestToken(providerName)] = requestToken

        RedirectHolder.setUri(params.redirectUrl)
        return redirect(url: url)
    }
}

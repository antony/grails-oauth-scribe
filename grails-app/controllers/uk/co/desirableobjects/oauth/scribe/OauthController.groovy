package uk.co.desirableobjects.oauth.scribe

import org.scribe.model.Verifier
import org.scribe.model.Token

class OauthController {

    private final Token EMPTY_TOKEN = null

    OauthService oauthService

    def callback = {

        Verifier verifier = extractVerifier(params)

        if (!verifier) {
            return redirect(uri: oauthService.failureUri)
        }

        Token requestToken = (Token) session[OauthService.REQUEST_TOKEN_SESSION_KEY]
        Token accessToken = oauthService.getAccessToken(requestToken, verifier)

        session[OauthService.ACCESS_TOKEN_SESSION_KEY] = accessToken
        session.removeAttribute(OauthService.REQUEST_TOKEN_SESSION_KEY)

        return redirect(uri: oauthService.successUri)

    }

    private extractVerifier(params) {
        
        String verifierKey = 'oauth_verifier'
        if (oauthService.oauthVersion==SupportedOauthVersion.TWO) {
            verifierKey = 'code'
        }

        if (!params[verifierKey]) {
             log.error("Cannot authenticate with oauth: Could not find oauth verifier in ${params}")
             return null
        }

        String verification = params[verifierKey]
        return new Verifier(verification)

    }

    def authenticate = {

        Token requestToken = EMPTY_TOKEN
        if (oauthService.getOauthVersion() == SupportedOauthVersion.ONE) {
            requestToken = oauthService.requestToken
        }

        session[OauthService.REQUEST_TOKEN_SESSION_KEY] = requestToken
        String url = oauthService.getAuthorizationUrl(requestToken)
        
        return redirect(url: url)
    }

}

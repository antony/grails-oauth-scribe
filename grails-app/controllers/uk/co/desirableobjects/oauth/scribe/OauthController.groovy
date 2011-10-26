package uk.co.desirableobjects.oauth.scribe

import org.scribe.model.Verifier
import org.scribe.model.Token

class OauthController {

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

        if (!params.oauth_verifier) {
             log.error("Cannot authenticate with oauth: Could not find oauth verifier in ${params}")
             return null
        }

        String verification = params.oauth_verifier
        return new Verifier(verification)

    }

    // TODO: I don't like how this uses the session
    def authenticate = {

        Token requestToken = oauthService.requestToken
        session[OauthService.REQUEST_TOKEN_SESSION_KEY] = requestToken
        String url = oauthService.getAuthorizationUrl(requestToken)

        return redirect(url: url)
    }

}

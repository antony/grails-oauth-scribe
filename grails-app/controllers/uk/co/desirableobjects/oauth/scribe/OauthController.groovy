package uk.co.desirableobjects.oauth.scribe

import org.scribe.model.Verifier
import org.scribe.model.Token

class OauthController {

    OauthService oauthService

    def callback = {

        String verification = params.oauth_verifier
        Verifier verifier = new Verifier(verification)

        Token requestToken = (Token) session[OauthService.REQUEST_TOKEN_SESSION_KEY]
        Token accessToken = oauthService.getAccessToken(requestToken, verifier)

        session.oauthAccessToken = accessToken
        redirect uri: oauthService.successUri

    }

    // TODO: I don't like how this uses the session
    def authenticate = {

        Token requestToken = oauthService.requestToken
        session[OauthService.REQUEST_TOKEN_SESSION_KEY] = requestToken
        String url = oauthService.getAuthorizationUrl(requestToken)

        return redirect(url: url)
    }

}

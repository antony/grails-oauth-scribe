package uk.co.desirableobjects.oauth.scribe

import org.scribe.model.Verifier
import org.scribe.model.Token

class OauthController {

    OauthService oauthService

    def callback = {

        String verification = params.oauth_verifier
        Verifier verifier = new Verifier(verification)

        // TODO: is this right? do we request it again?
        Token accessToken = oauthService.getAccessToken(oauthService.requestToken, verifier)

        session.oauthAccessToken = accessToken

    }

    def authenticate = {

        Token requestToken = oauthService.requestToken
        String url = oauthService.getAuthorizationUrl(requestToken)

        return redirect(url: url)
    }

}

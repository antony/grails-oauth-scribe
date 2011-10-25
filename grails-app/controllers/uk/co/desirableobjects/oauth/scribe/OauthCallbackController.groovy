package uk.co.desirableobjects.oauth.scribe

import org.scribe.model.Verifier
import org.scribe.model.Token

class OauthCallbackController {

    OauthService oauthService

    def callback = {

        String verification = params.oauth_verifier

        Verifier verifier = new Verifier(verification)

        // TODO: is this right? do we request it again?
        Token accessToken = oauthService.getAccessToken(oauthService.requestToken, verifier)

        session.oauthAccessToken = accessToken

    }

}

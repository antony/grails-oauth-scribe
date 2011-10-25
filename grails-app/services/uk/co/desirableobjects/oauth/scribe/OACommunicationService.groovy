package uk.co.desirableobjects.oauth.scribe

import org.scribe.model.Token
import org.scribe.model.Verb
import org.scribe.model.OAuthRequest
import org.scribe.oauth.OAuthService
import org.scribe.model.Response

class OACommunicationService {

    static transactional = true

    Response accessResource(OAuthService service, Token accessToken, Verb verb, String url) {

        OAuthRequest oAuthRequest = new OAuthRequest(verb, url)
        service.signRequest(accessToken, oAuthRequest)
        return oAuthRequest.send()

    }
}

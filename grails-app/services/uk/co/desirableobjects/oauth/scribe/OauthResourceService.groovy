package uk.co.desirableobjects.oauth.scribe

import org.scribe.model.Token
import org.scribe.model.Verb
import org.scribe.model.OAuthRequest
import org.scribe.oauth.OAuthService
import org.scribe.model.Response
import java.util.concurrent.TimeUnit

class OauthResourceService {

    static def transactional = false

    Response accessResource(OAuthService service, Token accessToken, Verb verb, String url, int connectTimeout, int receiveTimeout) {

        OAuthRequest oAuthRequest = new OAuthRequest(verb, url)
        oAuthRequest.setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
        oAuthRequest.setReadTimeout(receiveTimeout, TimeUnit.MILLISECONDS)
        service.signRequest(accessToken, oAuthRequest)
        return oAuthRequest.send()

    }
}

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
        return accessResource(service, accessToken, verb, url, null as Map, connectTimeout, receiveTimeout)
    }

    Response accessResource(OAuthService service, Token accessToken, Verb verb, String url, Map body, int connectTimeout, int receiveTimeout) {

        OAuthRequest req = buildOauthRequest(verb, url, connectTimeout, receiveTimeout)
        body?.each {String k, String v->
            req.addBodyParameter(k, v)
        }
        return signAndSend(service, accessToken, req)

    }

    Response accessResource(OAuthService service, Token accessToken, Verb verb, String url, String payload, String contentType, int connectTimeout, int receiveTimeout) {

        OAuthRequest req = buildOauthRequest(verb, url, connectTimeout, receiveTimeout)
        req.addPayload(payload)
        req.addHeader("Content-Length", Integer.toString(payload.length()))
        req.addHeader("Content-Type", contentType)
        return signAndSend(service, accessToken, req)

    }

    private OAuthRequest buildOauthRequest(Verb verb, String url, int connectTimeout, int receiveTimeout) {

        OAuthRequest req = new OAuthRequest(verb, url)
        req.setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
        req.setReadTimeout(receiveTimeout, TimeUnit.MILLISECONDS)
        return req

    }

    private Response signAndSend(OAuthService service, Token accessToken, OAuthRequest req) {

        service.signRequest(accessToken, req)
        return req.send()

    }
}

package uk.co.desirableobjects.oauth.scribe

import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.model.Response
import com.github.scribejava.core.model.Token
import com.github.scribejava.core.model.Verb
import com.github.scribejava.core.oauth.OAuthService
import uk.co.desirableobjects.oauth.scribe.resource.ResourceAccessor

class OauthResourceService {

    Response accessResource(OAuthService service, Token accessToken, ResourceAccessor ra) {
        OAuthRequest req = buildOauthRequest(ra.verb, ra.url)
        if (ra.payload) {
            req.setPayload(ra.payload)
        }
        ra.headers.each { String name, String value ->
            req.addHeader(name, value)
        }
        ra.bodyParameters?.each { String k, String v ->
            req.addBodyParameter(k, v)
        }
        ra.querystringParams?.each { String name, String value ->
            req.addQuerystringParameter(name, value)
        }
        return signAndSend(service, accessToken, req)
    }

    private OAuthRequest buildOauthRequest(Verb verb, String url) {
        return new OAuthRequest(verb, url)
    }

    private Response signAndSend(OAuthService service, Token accessToken, OAuthRequest req) {
        service.signRequest(accessToken, req)
        return service.execute(req)
    }
}
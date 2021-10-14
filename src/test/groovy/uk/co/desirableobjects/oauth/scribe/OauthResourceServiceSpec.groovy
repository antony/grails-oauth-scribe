package uk.co.desirableobjects.oauth.scribe

import com.github.scribejava.core.model.*
import com.github.scribejava.core.oauth.OAuthService
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import uk.co.desirableobjects.oauth.scribe.resource.ResourceAccessor

class OauthResourceServiceSpec extends Specification implements ServiceUnitTest<OauthResourceService> {

    def 'User can set headers on oauth request'() {
        given:
        OAuthService parent = Mock(OAuthService)

        when:
        ResourceAccessor resourceAccessor = new ResourceAccessor()
        resourceAccessor.with {
            verb = Verb.GET
            url = 'http://example.net/res'
            payload = 'Test'.bytes
            bodyParameters = [x: 'y']
            addHeader 'Accept', 'application/pdf'
        }
        service.accessResource(parent, new OAuth2AccessToken('token', 'secret'), resourceAccessor)

        then:
        1 * parent.signRequest(new OAuth2AccessToken('token', 'secret'), { OAuthRequest req ->
            req.verb == Verb.GET
            req.headers == ['Content-Length': '4', 'Accept': 'application/pdf']
            req.url == 'http://example.net/res'
            req.getStringPayload() == 'Test'
            req.bodyParams.size() == 1
            req.bodyParams.contains(new Parameter('x', 'y'))
        } as OAuthRequest)
        1 * parent.execute({ OAuthRequest req ->
            req.verb == Verb.GET
            req.headers == ['Content-Length': '4', 'Accept': 'application/pdf']
            req.url == 'http://example.net/res'
            req.getStringPayload() == 'Test'
            req.bodyParams.size() == 1
            req.bodyParams.contains(new Parameter('x', 'y'))
        } as OAuthRequest)
        0 * _

    }

    def 'null payload should be gracefully handled to avoid NPE'() {

        given:
        OAuthService parent = Mock(OAuthService)

        when:
        ResourceAccessor resourceAccessor = new ResourceAccessor()
        resourceAccessor.with {
            verb = Verb.GET
            url = 'http://example.net/res'
            //payload = null
            bodyParameters = [x: 'y']
            addHeader 'Accept', 'application/pdf'
        }
        assert resourceAccessor.payload == null
        service.accessResource(parent, new OAuth2AccessToken('token', 'secret'), resourceAccessor)

        then:
        1 * parent.signRequest(new OAuth2AccessToken('token', 'secret'), { OAuthRequest req ->
            req.verb == Verb.GET
            req.headers == ['Content-Length': '4', 'Accept': 'application/pdf']
            req.url == 'http://example.net/res'
            req.getStringPayload() == 'Test'
            req.bodyParams.size() == 1
            req.bodyParams.contains(new Parameter('x', 'y'))
        } as OAuthRequest)
        1 * parent.execute({ OAuthRequest req ->
            req.verb == Verb.GET
            req.headers == ['Content-Length': '4', 'Accept': 'application/pdf']
            req.url == 'http://example.net/res'
            req.getStringPayload() == 'Test'
            req.bodyParams.size() == 1
            req.bodyParams.contains(new Parameter('x', 'y'))
        } as OAuthRequest)
        0 * _

    }

    def 'query string parameters should be correctly added to a request when available'() {

        given:
        OAuthService theParent = Mock(OAuthService)
        Token aToken = new OAuth2AccessToken('token', 'secret')

        when: "the resource accessor has query string parameters"
        def resourceAccessor = new ResourceAccessor()
        resourceAccessor.with {
            verb = Verb.GET
            url = 'http://example.net/res'
            querystringParams = ["value1": "firstValue", "value2": "secondValue"]
        }
        service.accessResource(theParent, aToken, resourceAccessor)

        then: "the parent signs a request with the correct query string params"
        1 * theParent.signRequest(_ as Token, { OAuthRequest req ->
            req.queryStringParams.asFormUrlEncodedString() == "value1=firstValue&value2=secondValue"
        } as OAuthRequest)

        when: "the resource accessor has no query string parameters"
        resourceAccessor = new ResourceAccessor()
        resourceAccessor.with {
            verb = Verb.GET
            url = 'http://example.net/res'
        }
        service.accessResource(theParent, aToken, resourceAccessor)

        then: "the parent signs a request without any query string params"
        1 * theParent.signRequest(_ as Token, { OAuthRequest req ->
            req.queryStringParams.size() == 0
        } as OAuthRequest)
    }
}
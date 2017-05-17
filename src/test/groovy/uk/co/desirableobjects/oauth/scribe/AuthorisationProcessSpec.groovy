package uk.co.desirableobjects.oauth.scribe

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import com.github.scribejava.core.model.Response
import com.github.scribejava.core.model.OAuth1AccessToken
import com.github.scribejava.core.model.OAuth1RequestToken
import com.github.scribejava.core.model.OAuth2AccessToken
import com.github.scribejava.core.model.OAuthConfig
import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.model.Token
import com.github.scribejava.core.oauth.OAuthService
import com.github.scribejava.core.oauth.OAuth10aService
import org.springframework.http.HttpStatus
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Unroll
import uk.co.desirableobjects.oauth.scribe.exception.UnknownProviderException
import uk.co.desirableobjects.oauth.scribe.resource.ResourceAccessor

@Stepwise
@TestMixin(GrailsUnitTestMixin)
class AuthorisationProcessSpec extends Specification {

    private static final String DUMMY_OAUTH_RESOURCE_URI = 'http://example.org/list'
        @Shared Token requestToken
        @Shared Token accessToken
        @Shared OauthService oaService

        // TODO: This scenario: oauth_problem=signature_invalid&oauth_problem_advice=Failed%20to%20validate%20signature
        def 'a request token can be fetched'() {

                given:

                    oaService = new OauthService()
                    oaService.grailsApplication = [config: [
                            oauth: [
                                providers: [
                                    twitter: [
                                        api: com.github.scribejava.apis.TwitterApi,
                                        key: "myKey",
                                        secret: "mySecret" ] ]]]]
                    oaService.afterPropertiesSet()

                    oaService.services.twitter.service = new MockOAuth1Service()

                when:
                    requestToken = oaService.getTwitterRequestToken()

                then:
                    requestToken.rawResponse == 'c'

        }

        def 'make the user validate our token'() {

            given:
                oaService.services.twitter.service = Mock(OAuth10aService)
                requestToken = new OAuth1RequestToken('a', 'b', 'c')

            when:
                String authUrl = oaService.getTwitterAuthorizationUrl(requestToken)

            then:
                1 * oaService.services.twitter.service.getAuthorizationUrl(requestToken) >> { return 'http://example.org/auth' }
                0 * _

            and:
                authUrl == 'http://example.org/auth'

        }

        def 'get the access token'() {

            given:
                String verifier = 'abcde'
                oaService.services.twitter.service = new MockOAuth1Service()

            when:
                accessToken = oaService.getTwitterAccessToken(requestToken, verifier)

           then:
                accessToken.rawResponse == 'f'

        }

        // TODO: Why do we fetch and pass the token - if we don't pass it, you could automatically get it?
        @Unroll
        def 'make a #verb request using the authorised connection with an xml payload'() {

            given:
                oaService.services.twitter.service = Mock(OAuthService)
                oaService.oauthResourceService = Mock(OauthResourceService)

            and:
                String expectedResponse = 'Hello There.'

            when:
                String body = null
                int code = -1

            and:
                def actualResponse = oaService."${verb}TwitterResourceWithPayload"(accessToken, DUMMY_OAUTH_RESOURCE_URI, payload, headers)
                println "??" + actualResponse.code
                body = actualResponse.body
                code = actualResponse.code

            then:
                1 * oaService.oauthResourceService.accessResource(oaService.services.twitter.service, accessToken, { ResourceAccessor ra ->
                    ra.verb == verb
                    ra.url == DUMMY_OAUTH_RESOURCE_URI
                    ra.payload = payload?.bytes
                } as ResourceAccessor) >> {
                    return new Response(HttpStatus.OK.value(), '', [:], expectedResponse)
                }
                0 * _

            and:

                code == HttpStatus.OK.value()
                body == expectedResponse

            where:

                verb      | payload                 | headers
                'get'     | '<xml><tag /></xml>'    | ['Content-Length': 15, 'Content-Type': 'application/xml']
                'post'    | '<xml><tag /></xml>'    | [:]
                'delete'  | 'xyzabc'                | ['Content-Type': 'application/xml', Accept: 'application/pdf' ]

        }

    @Unroll
    def 'make a #verb request using the authorised connection'() {

        given:
            oaService.services.twitter.service = Mock(OAuthService)
            oaService.oauthResourceService = Mock(OauthResourceService)

        and:
            String expectedResponse = 'Hello There.'

        when:
            String body = null
            int code = -1

        and:
            def actualResponse = oaService."${verb}TwitterResource"(accessToken, DUMMY_OAUTH_RESOURCE_URI, [a: 'b'], [Accept: 'application/pdf', 'Content-Type': 'application/json'])
            body = actualResponse.body
            code = actualResponse.code

        then:
            1 * oaService.oauthResourceService.accessResource(oaService.services.twitter.service, accessToken, { ResourceAccessor ra ->
                ra.verb == verb
                ra.url == DUMMY_OAUTH_RESOURCE_URI
                ra.bodyParameters == [a: 'b']
                ra.headers == [Accept: 'application/pdf', 'Content-Type': 'application/json']
            } as ResourceAccessor) >> {
                return new Response(HttpStatus.OK.value(), '', [:], expectedResponse)
            }
            0 * _

        and:
            code == HttpStatus.OK.value()
            body == expectedResponse

        where:
            verb << ['delete', 'post', 'get']

    }


    def 'try to call an invalid resource accessor method on the service' () {

            when:

                oaService.failResource(accessToken, 'anyUrl')

            then:

                thrown(MissingMethodException)

            when:

                oaService.punchTwitterResource(accessToken, 'anyUrl')

            then:

                thrown(MissingMethodException)

            when:

                oaService.putWonkyResource(accessToken, 'anyUrl')

            then:

                thrown(UnknownProviderException)


        }

}

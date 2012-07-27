package uk.co.desirableobjects.oauth.scribe

import spock.lang.Stepwise
import org.scribe.model.Token
import spock.lang.Shared
import org.scribe.oauth.OAuthService
import grails.plugin.spock.UnitSpec
import org.scribe.model.Verifier

import org.scribe.model.Verb
import org.scribe.model.Response
import org.springframework.http.HttpStatus
import spock.lang.Unroll
import uk.co.desirableobjects.oauth.scribe.exception.UnknownProviderException

@Stepwise
@Mixin(GMockAddon)
class AuthorisationProcessSpec extends UnitSpec {

    private static final String DUMMY_OAUTH_RESOURCE_URI = 'http://example.org/list'
    @Shared Token requestToken
        @Shared Token accessToken
        @Shared OauthService oaService

        // TODO: This scenario: oauth_problem=signature_invalid&oauth_problem_advice=Failed%20to%20validate%20signature
        def 'a request token can be fetched'() {

                given:

                    mockConfig """
                        import org.scribe.builder.api.TwitterApi

                        oauth {
                            providers {
                                twitter {
                                    api = TwitterApi
                                    key = 'myKey'
                                    secret = 'mySecret'
                                }
                            }
                        }
                    """
                    oaService = new OauthService()
                    oaService.grailsApplication = [config: [
                            oauth: [
                                providers: [
                                    twitter: [
                                        api: org.scribe.builder.api.TwitterApi,
                                        key: "myKey",
                                        secret: "mySecret" ] ]]]]
                    oaService.afterPropertiesSet()

                    oaService.services.twitter.service = mock(OAuthService)
                    oaService.services.twitter.service.getRequestToken().returns(new Token('a', 'b', 'c'))

                when:
                    simulate {
                        requestToken = oaService.getTwitterRequestToken()
                    }

                then:
                    requestToken.rawResponse == 'c'

        }

        def 'make the user validate our token'() {

            given:

                oaService.services.twitter.service.getAuthorizationUrl(requestToken).returns('http://example.org/auth')

            expect:

                simulate {
                    oaService.getTwitterAuthorizationUrl(requestToken) == 'http://example.org/auth'
                }

        }

        def 'get the access token'() {

            given:

                Token expectedToken = new Token('d', 'e', 'f')
                Verifier verifier = new Verifier('abcde')
                oaService.services.twitter.service = mock(OAuthService)
                oaService.services.twitter.service.getAccessToken(requestToken, verifier).returns(expectedToken)

            when:

                simulate {
                    accessToken = oaService.getTwitterAccessToken(requestToken, verifier)
                }

           then:

                accessToken.rawResponse == 'f'

        }

        // TODO: Why do we fetch and pass the token - if we don't pass it, you could automatically get it?
        @Unroll
        def 'make a #verb request using the authorised connection'() {

            given:

                oaService.services['twitter'].service = mock(OAuthService)

            and:
            
                String expectedResponse = 'Hello There.'
                Response oaResponse = mock(Response)
                oaResponse.getBody().returns(expectedResponse)
                oaResponse.getCode().returns(HttpStatus.OK.value())
            
            and:

                oaService.oauthResourceService = mock(OauthResourceService)
                oaService.oauthResourceService.accessResource(oaService.services['twitter'].service, accessToken, verb, DUMMY_OAUTH_RESOURCE_URI, 30000, 30000).returns(oaResponse)

            when:

                String body = null
                int code = -1

            and:

                simulate {

                    def actualResponse = oaService."${verb.name().toLowerCase()}TwitterResource"(accessToken, DUMMY_OAUTH_RESOURCE_URI)
                    body = actualResponse.body
                    code = actualResponse.code

                }

            then:

                code == HttpStatus.OK.value()
                body == expectedResponse

            where:

                verb << Verb.values()


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

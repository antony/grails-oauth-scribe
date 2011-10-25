package uk.co.desirableobjects.oauth.scribe

import spock.lang.Stepwise
import org.scribe.model.Token
import spock.lang.Shared
import org.scribe.oauth.OAuthService
import grails.plugin.spock.UnitSpec

@Stepwise
@Mixin(GMockAddon)
class AuthorisationProcessSpec extends UnitSpec {

        @Shared Token token
        @Shared OauthService oaService

        def 'a request token can be fetched'() {

                given:

                    mockConfig """
                        import org.scribe.builder.api.TwitterApi

                        oauth {
                            provider = TwitterApi
                            key = 'myKey'
                            secret = 'mySecret'
                        }
                    """
                    oaService = new OauthService()

                    oaService.service = mock(OAuthService)
                    oaService.service.getRequestToken().returns(new Token('a', 'b', 'c'))

                when:
                    simulate {
                        token = oaService.requestToken
                    }

                then:
                    token.rawResponse == 'c'

        }

        def 'make the user validate our token'() {

            given:

                oaService.service.getAuthorizationUrl(token).returns('http://example.org/auth')

            expect:

                simulate {
                    oaService.getAuthorizationUrl(token) == 'http://example.org/auth'
                }

        }

}

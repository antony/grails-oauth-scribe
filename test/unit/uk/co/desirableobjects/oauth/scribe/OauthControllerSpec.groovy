package uk.co.desirableobjects.oauth.scribe

import grails.plugin.spock.ControllerSpec
import org.scribe.model.Token
import org.scribe.model.Verifier

@Mixin(GMockAddon)
class OauthControllerSpec extends ControllerSpec {

    def 'Token can be read from callback'() {

        given:

            Token requestToken = new Token('a', 'b', 'c')
            Token accessToken = new Token('d', 'e', 'f')
            Verifier verifier = new Verifier('xyz')

        and:

            controller.oauthService = mock(OauthService)
            controller.oauthService.getRequestToken().returns(requestToken)
            controller.oauthService.getAccessToken(requestToken, match { it.value == verifier.value }).returns(accessToken)

        and:

            mockParams.oauth_verifier = verifier.value

        when:

            simulate {
                controller.callback()
            }

        then:

            mockSession.oauthAccessToken == accessToken

    }

    def 'Auth endpoint is hit'() {


        given:

            mockConfig """
                    import org.scribe.builder.api.TwitterApi

                    oauth {
                        provider = TwitterApi
                        key = 'myKey'
                        secret = 'mySecret'
                        callbackUrl = 'http://welcome.back/to/my/app'
                    }
            """

        and:
            Token requestToken = new Token('a', 'b', 'c')
            controller.oauthService = mock(OauthService)
            controller.oauthService.requestToken.returns(requestToken)
            controller.oauthService.getAuthorizationUrl(requestToken).returns('http://authorisation.url/auth')

        when:

            simulate {
                controller.authenticate()
            }

        then:

            redirectArgs.url == 'http://authorisation.url/auth'

    }

}

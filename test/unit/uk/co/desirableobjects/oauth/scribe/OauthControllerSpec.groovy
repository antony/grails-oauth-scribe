package uk.co.desirableobjects.oauth.scribe

import grails.plugin.spock.ControllerSpec
import org.scribe.model.Token
import org.scribe.model.Verifier

@Mixin(GMockAddon)
class OauthControllerSpec extends ControllerSpec {

    def 'Token can be read from callback'() {

        given:

            mockConfig '''

                oauth {
                    successUri = '/coffee/tea'
                }

            '''

        and:
            Token requestToken = new Token('a', 'b', 'c')
            mockSession['oasRequestToken'] = requestToken

        and:

            Token accessToken = new Token('d', 'e', 'f')
            Verifier verifier = new Verifier('xyz')

        and:

            controller.oauthService = mock(OauthService)
            controller.oauthService.getAccessToken(requestToken, match { it.value == verifier.value }).returns(accessToken)
            controller.oauthService.getSuccessUri().returns('/coffee/tea')

        and:

            mockParams.oauth_verifier = verifier.value

        when:

            simulate {
                controller.callback()
            }

        then:

            mockSession.oauthAccessToken == accessToken
            redirectArgs.uri == '/coffee/tea'

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


            mockSession['oasRequestToken'] == requestToken
            redirectArgs.url == 'http://authorisation.url/auth'

    }

}

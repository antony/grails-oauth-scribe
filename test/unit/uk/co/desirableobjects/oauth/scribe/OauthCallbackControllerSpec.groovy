package uk.co.desirableobjects.oauth.scribe

import grails.plugin.spock.ControllerSpec
import org.scribe.model.Token
import org.scribe.model.Verifier

@Mixin(GMockAddon)
class OauthCallbackControllerSpec extends ControllerSpec {

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

}

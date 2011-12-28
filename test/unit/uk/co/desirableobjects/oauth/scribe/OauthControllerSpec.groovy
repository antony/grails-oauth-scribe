package uk.co.desirableobjects.oauth.scribe

import grails.plugin.spock.ControllerSpec
import org.scribe.model.Token
import org.scribe.model.Verifier

@Mixin(GMockAddon)
class OauthControllerSpec extends ControllerSpec {

    def 'Success URL is hit and token is read from callback'() {

        given:
            Token requestToken = new Token('a', 'b', 'c')
            mockSession[OauthService.REQUEST_TOKEN_SESSION_KEY] = requestToken

        and:

            Token accessToken = new Token('d', 'e', 'f')
            Verifier verifier = new Verifier('xyz')

        and:

            controller.oauthService = mock(OauthService)
            controller.oauthService.getOauthVersion().returns(SupportedOauthVersion.ONE)
            controller.oauthService.getAccessToken(requestToken, match { it.value == verifier.value }).returns(accessToken)
            controller.oauthService.getSuccessUri().returns('/coffee/tea')

        and:

            mockParams.oauth_verifier = verifier.value

        when:

            simulate {
                controller.callback()
            }

        then:

            !mockSession[OauthService.REQUEST_TOKEN_SESSION_KEY]
            mockSession[OauthService.ACCESS_TOKEN_SESSION_KEY] == accessToken
            redirectArgs.uri == '/coffee/tea'

    }

    def 'callback provides no verifier'() {

        given:

            controller.oauthService = mock(OauthService)
            controller.oauthService.getOauthVersion().returns(SupportedOauthVersion.ONE)
            controller.oauthService.getFailureUri().returns('/coke/pepsi')

        when:

            simulate {
                controller.callback()
            }

        then:

            redirectArgs.uri == '/coke/pepsi'

    }

    def 'Authentication endpoint is hit'() {

        given:

            Token requestToken = new Token('a', 'b', 'c')
            controller.oauthService = mock(OauthService)
            controller.oauthService.getOauthVersion().returns(SupportedOauthVersion.ONE)
            controller.oauthService.requestToken.returns(requestToken)
            controller.oauthService.getAuthorizationUrl(requestToken).returns('http://authorisation.url/auth')

        when:

            simulate {
                controller.authenticate()
            }

        then:

            mockSession[OauthService.REQUEST_TOKEN_SESSION_KEY] == requestToken
            redirectArgs.url == 'http://authorisation.url/auth'

    }

    def 'In Oauth 2, request token endpoint is not hit'() {

        given:

            controller.oauthService = mock(OauthService)
            controller.oauthService.getOauthVersion().returns(SupportedOauthVersion.TWO)
            controller.oauthService.getAuthorizationUrl(null).returns('http://authorisation.url/auth')

        when:

            simulate {
                controller.authenticate()
            }

        then:

            mockSession[OauthService.REQUEST_TOKEN_SESSION_KEY] == null
            redirectArgs.url == 'http://authorisation.url/auth'

    }

    // TODO:  {"error":{"message":"Error validating client secret.","type":"OAuthException"}}

}

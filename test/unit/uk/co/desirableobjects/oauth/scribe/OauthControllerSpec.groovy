package uk.co.desirableobjects.oauth.scribe

import org.scribe.model.Token
import org.scribe.model.Verifier
import uk.co.desirableobjects.oauth.scribe.exception.UnknownProviderException
import org.scribe.oauth.OAuthService
import grails.test.mixin.TestFor
import spock.lang.Specification
import spock.lang.Shared

//@Mixin(GMockAddon)
@TestFor(OauthController)
class OauthControllerSpec extends Specification {

    private static final String REQUEST_TOKEN_SESSION_KEY = 'twitter:oasRequestToken'
    private static final String ACCESS_TOKEN_SESSION_KEY = 'twitter:oasAccessToken'
    private static final String PROVIDER_NAME = 'twitter'

    @Shared OauthProvider provider
    @Shared OAuthService service
    
    def setup() {

        service = Mock(OAuthService)
        provider = new OauthProvider(service: service, failureUri: '/coke/pepsi', successUri: '/coffee/tea')
        controller.oauthService = Mock(OauthService)

    }
    
    def 'If authenticate url is hit without a provider, an exception is thrown'() {

        when:
            controller.authenticate()

        then:
            controller.oauthService.findProviderConfiguration(null) >> { throw new UnknownProviderException(null) }

        and:
            thrown UnknownProviderException

    }

    def 'Success URL is hit and token is read from callback'() {

        given:

            Token requestToken = new Token('a', 'b', 'c')
            controller.session[REQUEST_TOKEN_SESSION_KEY] = requestToken

        and:

            Token accessToken = new Token('d', 'e', 'f')
            Verifier verifier = new Verifier('xyz')

        and:

            controller.params.provider = PROVIDER_NAME
            controller.params.oauth_verifier = verifier.value

        when:

            controller.callback()

        then:

            controller.oauthService.findSessionKeyForRequestToken(PROVIDER_NAME) >> { return REQUEST_TOKEN_SESSION_KEY }
            controller.oauthService.findSessionKeyForAccessToken(PROVIDER_NAME) >> { return ACCESS_TOKEN_SESSION_KEY }
            controller.oauthService.getAccessToken(PROVIDER_NAME, requestToken, _ as Verifier) >> { return accessToken }
            controller.oauthService.findProviderConfiguration(PROVIDER_NAME) >> { return provider }

        then:

            !session[REQUEST_TOKEN_SESSION_KEY]
            session[ACCESS_TOKEN_SESSION_KEY] == accessToken
            controller.response.redirectUrl == '/coffee/tea'

    }

    def 'callback provides no verifier'() {

        when:

            controller.params.provider = PROVIDER_NAME
            controller.callback()

        then:

            provider.service.version >> { return '1.0' }
            controller.oauthService.findProviderConfiguration(PROVIDER_NAME) >> { return provider }
            controller.response.redirectUrl == '/coke/pepsi'

    }

    def 'Authentication endpoint is hit'() {

        given:

            Token requestToken = new Token('a', 'b', 'c')
            service.getRequestToken() >> { return requestToken }

        when:

            controller.params.provider = PROVIDER_NAME
            controller.authenticate()

        then:

            controller.oauthService.findProviderConfiguration(PROVIDER_NAME) >> { return provider }
            controller.oauthService.findSessionKeyForRequestToken(PROVIDER_NAME) >> { return REQUEST_TOKEN_SESSION_KEY }
            provider.service.version >> { return '1.0' }
            controller.oauthService.getAuthorizationUrl(PROVIDER_NAME, requestToken) >> { return 'http://authorisation.url/auth' }

        and:

            session[REQUEST_TOKEN_SESSION_KEY] == requestToken
            controller.response.redirectUrl == 'http://authorisation.url/auth'

    }

    def 'In Oauth 2, request token endpoint is not hit'() {

        when:

            controller.params.provider = PROVIDER_NAME
            controller.authenticate()

        then:

            controller.oauthService.findProviderConfiguration(PROVIDER_NAME) >> { return provider }
            provider.service.version >> { return '2.0' }
            controller.oauthService.findSessionKeyForRequestToken(PROVIDER_NAME) >> { return REQUEST_TOKEN_SESSION_KEY }
            controller.oauthService.getAuthorizationUrl(PROVIDER_NAME, null) >> { return 'http://authorisation.url/auth' }

        and:
            !session[REQUEST_TOKEN_SESSION_KEY]
            controller.response.redirectUrl == 'http://authorisation.url/auth'

    }

    def 'Default success and failure URLs are sane'() {

    }

    // TODO:  {"error":{"message":"Error validating client secret.","type":"OAuthException"}}

}

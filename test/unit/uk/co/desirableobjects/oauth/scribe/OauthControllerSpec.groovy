package uk.co.desirableobjects.oauth.scribe

import spock.lang.Unroll
import spock.lang.Shared
import grails.test.mixin.TestFor
import org.scribe.model.Token
import spock.lang.Specification
import org.scribe.model.Verifier
import org.scribe.oauth.OAuthService
import uk.co.desirableobjects.oauth.scribe.holder.RedirectHolder
import org.springframework.web.context.request.RequestContextHolder
import uk.co.desirableobjects.oauth.scribe.exception.UnknownProviderException
import uk.co.desirableobjects.oauth.scribe.exception.MissingRequestTokenException

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
            RedirectHolder.getRedirect() == RedirectHolder.getDefaultRedirect()
            session[REQUEST_TOKEN_SESSION_KEY] == requestToken
            controller.response.redirectUrl == 'http://authorisation.url/auth'

    }

    def 'Authentication endpoint is hit with valid redirect uri'() {
        def redirectUri = "/controller/action/id"

        given:
            def requestToken = new Token('a', 'b', 'c')
            service.getRequestToken() >> { return requestToken }
            controller.params.redirectUrl = redirectUri
        when:
            controller.params.provider = PROVIDER_NAME
            controller.authenticate()
        then:
            controller.oauthService.findProviderConfiguration(PROVIDER_NAME) >> { return provider }
            controller.oauthService.findSessionKeyForRequestToken(PROVIDER_NAME) >> { return REQUEST_TOKEN_SESSION_KEY }
            provider.service.version >> { return '1.0' }
            controller.oauthService.getAuthorizationUrl(PROVIDER_NAME, requestToken) >> { return 'http://authorisation.url/auth' }
        and:
            RedirectHolder.getRedirect().get(RedirectHolder.URI_NAME) == redirectUri
            session[REQUEST_TOKEN_SESSION_KEY] == requestToken
            controller.response.redirectUrl == 'http://authorisation.url/auth'
    }

    def 'In Oauth 2, request token endpoint is not hit'() {

        given:
            Token emptyToken = controller.EMPTY_TOKEN
            controller.params.provider = PROVIDER_NAME

        when:

            controller.authenticate()

        then:

            controller.oauthService.findProviderConfiguration(PROVIDER_NAME) >> { return provider }
            provider.service.version >> { return '2.0' }
            controller.oauthService.findSessionKeyForRequestToken(PROVIDER_NAME) >> { return REQUEST_TOKEN_SESSION_KEY }
            controller.oauthService.getAuthorizationUrl(PROVIDER_NAME, emptyToken) >> { return 'http://authorisation.url/auth' }

        and:
            RedirectHolder.getRedirect() == RedirectHolder.getDefaultRedirect()
            session[REQUEST_TOKEN_SESSION_KEY] == emptyToken
            controller.response.redirectUrl == 'http://authorisation.url/auth'

    }

    @Unroll
    def 'Oauth callback is hit but there is no request token in the session (bad callback domain) for oauth #oauthVersion'() {

        given:
            controller.params.provider = PROVIDER_NAME
            controller.params.oauth_verifier = 'oauth-verifier'
            controller.params.code = 'verifier-key'

        when:
            controller.callback()

        then:
            controller.oauthService.findSessionKeyForRequestToken(PROVIDER_NAME) >> { return REQUEST_TOKEN_SESSION_KEY }
            controller.oauthService.findProviderConfiguration(PROVIDER_NAME) >> { return provider }
            provider.service.version >> { return oauthVersion }

        and:
            def exception = thrown MissingRequestTokenException
            exception.message == "We couldn't find a request token for twitter in the session. A common cause of this is that you have been given a new session by the servlet container because your callback domain is different to the domain you are authenticating from. Check that the domain name in the URL bar of your browser matches the domain name of your callback URL"

        where:
            oauthVersion = ['1.0', '2.0']

    }

    def 'RedirectHolder execute the method setUri(), set valid uri'() {
        def redirectUri = "http://test.com"

        when:
            RedirectHolder.setUri(redirectUri)
        then:
            def hash = RequestContextHolder.currentRequestAttributes()?.getSession()?.getAttribute(RedirectHolder.HASH_NAME)
            def uri = hash.get(RedirectHolder.URI_NAME)
            uri == redirectUri
    }

    def 'RedirectHolder execute the method setUri(), set valid uri with empty hash in session'() {
        def redirectUri = "http://test.com"

        given:
            def currentSession = RequestContextHolder.currentRequestAttributes()?.getSession()
            currentSession.putAt(RedirectHolder.HASH_NAME, [:])
        when:
            RedirectHolder.setUri(redirectUri)
        then:
            def hash = RequestContextHolder.currentRequestAttributes()?.getSession()?.getAttribute(RedirectHolder.HASH_NAME)
            def uri = hash.get(RedirectHolder.URI_NAME)
            uri == redirectUri
    }

    def 'RedirectHolder execute the method setUri(), set valid uri with custom hash in session'() {
        def redirectUri = "http://test.com"
        def testContent = "Test content"

        given:
            def currentSession = RequestContextHolder.currentRequestAttributes()?.getSession()
            currentSession.putAt(RedirectHolder.HASH_NAME, [testContent: testContent])
        when:
            RedirectHolder.setUri(redirectUri)
        then:
            def hash = RequestContextHolder.currentRequestAttributes()?.getSession()?.getAttribute(RedirectHolder.HASH_NAME)
            def uri = hash.get(RedirectHolder.URI_NAME)
            uri == redirectUri
    }

    def 'RedirectHolder execute the method setUri(), set invalid uri'() {
        def invalidRedirectUri = ""

        when:
        RedirectHolder.setUri(invalidRedirectUri)
        then:
        RequestContextHolder.currentRequestAttributes()?.getSession()?.getAttribute(RedirectHolder.HASH_NAME) == null
        RedirectHolder.getRedirect() == RedirectHolder.getDefaultRedirect()
    }

    def 'RedirectHolder execute the method getRedirect(), return valid hash'() {
        def redirectUri = "http://test.com"
        def hash = [:]
        hash.put(RedirectHolder.URI_NAME, redirectUri)

        given:
            RedirectHolder.setUri(redirectUri)
        when:
            def ex = RedirectHolder.getRedirect()
        then:
            ex == hash
    }

    def 'RedirectHolder execute the method getRedirect(), return redirect with custom uri'() {
        def redirectUri = "http://test.com"
        def hash = [:]
        hash.put(RedirectHolder.URI_NAME, redirectUri)

        given:
            RedirectHolder.setUri(redirectUri)
        when:
            def ex = RedirectHolder.getRedirect()
        then:
            ex == hash
    }

    def 'RedirectHolder execute the method getRedirect(), return default result'() {
        when:
            def ex = RedirectHolder.getRedirect()
        then:
            ex == RedirectHolder.getDefaultRedirect()
    }

    def 'RedirectHolder execute the method setRedirectHash(), set valid hash'() {
        def hash = [:]
        hash.put("controller", "object")
        hash.put("action", "show")
        hash.put("id", "1")

        when:
            RedirectHolder.setRedirectHash(hash)
        then:
            hash == RedirectHolder.getOrCreateRedirectHash()
    }

    def 'RedirectHolder execute the method setRedirectHash(), set invalid hash'() {
        when:
            RedirectHolder.setRedirectHash(null)
        then:
            RedirectHolder.getRedirect() == RedirectHolder.getDefaultRedirect()
    }

    def 'RedirectHolder execute the method getStorage(), return current session'() {
        when:
            def ex = RedirectHolder.getStorage()
        then:
            ex == RequestContextHolder.currentRequestAttributes()?.getSession()
    }

    def 'RedirectHolder execute the method getOrCreateRedirectHash(), return empty hash'() {
        when:
            def ex = RedirectHolder.getOrCreateRedirectHash()
        then:
            ex == [:]
    }

    def 'RedirectHolder execute the method getOrCreateRedirectHash(), return custom hash'() {
        def testContent = "Test content"
        def hash = [testContent: testContent]

        given:
            def currentSession = RequestContextHolder.currentRequestAttributes()?.getSession()
            currentSession.putAt(RedirectHolder.HASH_NAME, hash)
        when:
            def ex = RedirectHolder.getOrCreateRedirectHash()
        then:
            ex == hash
    }

    def 'RedirectHolder execute the method getDefaultRedirect(), return default redirect hash'() {
        given:
            def redirectHash = [:]
            redirectHash.put(RedirectHolder.URI_NAME, RedirectHolder.DEFAULT_URI)
        when:
            def ex = RedirectHolder.getDefaultRedirect()
        then:
            ex == redirectHash
    }

    // TODO: {"error":{"message":"Error validating client secret.","type":"OAuthException"}}
    // TODO: Catch and deal with timeouts in a sensible way.
}

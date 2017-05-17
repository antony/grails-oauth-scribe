package uk.co.desirableobjects.oauth.scribe

import grails.test.mixin.TestFor
import com.github.scribejava.apis.TwitterApi
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.builder.api.BaseApi
import com.github.scribejava.core.model.OAuthConfig
import com.github.scribejava.core.model.OAuth2AccessToken
import com.github.scribejava.core.model.SignatureType
import com.github.scribejava.core.model.Token
import com.github.scribejava.core.model.Verb
import com.github.scribejava.core.oauth.OAuthService
import spock.lang.Specification
import spock.lang.Unroll
import uk.co.desirableobjects.oauth.scribe.exception.InvalidOauthProviderException
import uk.co.desirableobjects.oauth.scribe.resource.ResourceAccessor

// This is a horrible hack. To disable automatic mocking and wiring of the
// OauthService bean (because we can't test the configuration handling otherwise)
// we deliberately mark this test case as for a different service. If the
// following line is remove, the tests will start failing with mysterious
// errors!
@TestFor(OauthResourceService)
class OauthServiceSpec extends Specification {

    def 'Configuration is missing'() {

        given:
            OauthService service = new OauthService()
            service.grailsApplication = [config: [:]]

        when:
            service.afterPropertiesSet()

        then:
            thrown IllegalStateException
    }

    def 'OAuthService can handle multiple providers'() {

        given:
            OauthService service = new OauthService()
            service.grailsApplication = [config: [
                    oauth: [
                        providers: [
                            twitter: [
                                api: com.github.scribejava.apis.TwitterApi,
                                key: "twitter",
                                secret: "identica" ],
                            facebook: [
                                api: com.github.scribejava.apis.FacebookApi,
                                key: "zuckerberg",
                                secret: "brothers" ] ]]]]
            service.afterPropertiesSet()

        expect:
            service.services.size() == 2

    }

    def 'OAuthService lower-cases provider names'() {

        given:
            OauthService service = new OauthService()
            service.grailsApplication = [config: [
                    oauth: [
                        providers: [
                            twitter: [
                                api: com.github.scribejava.apis.TwitterApi,
                                key: "twitter",
                                secret: "identica" ],
                            facebook: [
                                api: com.github.scribejava.apis.FacebookApi,
                                key: "zuckerberg",
                                secret: "brothers" ] ]]]]
            service.afterPropertiesSet()

        expect:
            service.services.keySet() == ['twitter', 'facebook'] as Set<String>

    }

    def 'Configuration contains valid provider Twitter'() {

        given:
            OauthService service = new OauthService()
            service.grailsApplication = [config: [
                    oauth: [
                        providers: [
                            mine: [
                                api: TwitterApi,
                                key: "myKey",
                                secret: "mySecret" ] ]]]]

        expect:
            service.afterPropertiesSet()

    }

    @Unroll()
    def 'Configuration enables debugging support when debug = #debug'() {

        given:

            boolean debugEnabled = false
            ServiceBuilder.metaClass.debug = { debugEnabled = true }

        and:
            OauthService service = new OauthService()
            service.grailsApplication = [config: [
                    oauth: [
                        providers: [
                            twitter: [
                                api: com.github.scribejava.apis.TwitterApi,
                                key: "myKey",
                                secret: "mySecret" ] ],
                        debug: debug ] ]]
            service.afterPropertiesSet()

        expect:
            debugEnabled == debug

        where:
            debug << [false, true]

    }

    @Unroll
    def 'Configuration includes or excludes scope when scope is #scope'() {

        given:

            String providedScope = null
            ServiceBuilder.metaClass.scope = { String passedScope -> providedScope = passedScope }

        and:
            OauthService service = new OauthService()
            service.grailsApplication = [config: [
                    oauth: [
                        providers: [
                            twitter: [
                                api: com.github.scribejava.apis.TwitterApi,
                                key: "myKey",
                                secret: "mySecret",
                                scope: scope ? 'testScope' : null ] ]]]]
            service.afterPropertiesSet()

        expect:
            providedScope == (scope ? 'testScope' : null)

        where:
            scope << [true, false]

    }


    void cleanup() {
        ServiceBuilder.metaClass = null
    }

    def 'Configuration contains a non-class as a provider'() {

        given:
            OauthService service = new OauthService()
            service.grailsApplication = [config: [
                    oauth: [
                        providers: [
                            invalid: [
                                api: 'some custom string',
                                key: "myKey",
                                secret: "mySecret" ] ]]]]

        when:
            service.afterPropertiesSet()

        then:
            thrown(InvalidOauthProviderException)

    }

    def 'Configuration contains a non-implementing class as a provider'() {

        given:
            OauthService service = new OauthService()
            service.grailsApplication = [config: [
                    oauth: [
                        providers: [
                            invalid: [
                                api: InvalidProviderApi,
                                key: "myKey",
                                secret: "mySecret" ] ]]]]

        when:
            service.afterPropertiesSet()

        then:
            thrown(InvalidOauthProviderException)

    }

    @Unroll
    def 'Configuration is provided with invalid key #key or secret #secret'() {


        given:
            OauthService service = new OauthService()
            service.grailsApplication = [config: [
                    oauth: [
                        providers: [
                            twitter: [
                                api: com.github.scribejava.apis.TwitterApi,
                                key: key,
                                secret: secret ],
                            facebook: [
                                api: com.github.scribejava.apis.FacebookApi,
                                key: "facebook-key",
                                secret: "facebook-secret" ] ]]]]

        when:
            service.afterPropertiesSet()

        then:
            thrown(IllegalStateException)

        where:
            key     | secret
            null    | "'secret'"
            "'key'" | null
            null    | null

    }

    // validate signature types
    def 'callback URL and Signature Type is supported but optional'() {

        given:
            OauthService service = new OauthService()
            service.grailsApplication = [config: [
                    oauth: [
                        providers: [
                            twitter: [
                                api: com.github.scribejava.apis.TwitterApi,
                                key: 'myKey',
                                secret: 'mySecret',
                                callback: 'http://example.com:1234/url',
                                signatureType: SignatureType.QueryString ] ]]]]

        expect:
            service.afterPropertiesSet() == null

    }

    def 'configuration contains a successUri and a failureUri'() {

        given:
            OauthService service = new OauthService()
            service.grailsApplication = [config: [
                    oauth: [
                        providers: [
                            twitter: [
                                api: com.github.scribejava.apis.TwitterApi,
                                key: 'myKey',
                                secret: 'mySecret',
                                successUri: '/coffee/tea',
                                failureUri: '/cola/pepsi' ] ]]]]

        when:
            service.afterPropertiesSet()

        then:
            service.services.twitter.successUri == '/coffee/tea'
            service.services.twitter.failureUri == '/cola/pepsi'

    }

    def 'configuration can set socket and receive timeouts'() {

        given:
            OauthService service = new OauthService()
            service.oauthResourceService = Mock(OauthResourceService)
            service.grailsApplication = [config: [
                    oauth: [
                        providers: [
                            twitter: [
                                api: com.github.scribejava.apis.TwitterApi,
                                key: 'myKey',
                                secret: 'mySecret' ]],
                        connectTimeout: 5000,
                        receiveTimeout: 5000 ]]]

        when:
            service.afterPropertiesSet()

        then:
            service.connectTimeout == 5000
            service.receiveTimeout == 5000

        when:
            service.getTwitterResource(new OAuth2AccessToken('myKey', 'mySecret'), 'http://www.example.com')

        then:
            1 * service.oauthResourceService.accessResource(_ as OAuthService, _ as Token, _ as ResourceAccessor)
            0 * _

    }


    def 'if connection and recieve timeouts are not set, they are defaulted to thirty seconds'() {

        given:
            OauthService service = new OauthService()
            service.grailsApplication = [config: [
                    oauth: [
                        providers: [
                            twitter: [
                                api: com.github.scribejava.apis.TwitterApi,
                                key: 'myKey',
                                secret: 'mySecret',
                                successUri: '/coffee/tea',
                                failureUri: '/cola/pepsi' ] ]]]]

        when:
            service.afterPropertiesSet()

        then:
            service.connectTimeout == 30000
            service.receiveTimeout == 30000

    }

    @Unroll
    def 'Service returns correct API version when given #apiClass'() {

        given:
            OauthService service = new OauthService()
            service.grailsApplication = [config: [
                    oauth: [
                        providers: [
                            dynamic: [
                                api: apiClass,
                                key: 'myKey',
                                secret: 'mySecret' ] ]]]]

        when:
            service.afterPropertiesSet()

        then:
            service.services.dynamic.oauthVersion == apiVersion

        where:
            apiClass                              | apiVersion
            'com.github.scribejava.apis.TwitterApi'   | SupportedOauthVersion.ONE
            'com.github.scribejava.apis.FacebookApi'  | SupportedOauthVersion.TWO

    }

	def "service correctly handles ResourceWithQuerystringParameters methods"() {

		given: "our service"
			OauthService service = new OauthService()
			OauthResourceService oauthResourceService = Mock(OauthResourceService)
			service.oauthResourceService = oauthResourceService
		and: "a mock provider"
			OauthProvider aProvider = Mock(OauthProvider)
			OAuthService theProviderService = Mock(OAuthService)
			aProvider.getService() >> theProviderService
			service.services = [twitter: aProvider]
		and: "the input parameters"
			def theToken = new OAuth2AccessToken("a", "b")
			def theUrl = "http://someapi.net/api"
			def theQuerystringParams = [param1:"value1", param2:"value2"]
			def theExtraHeaders = [header1:"valueA", header2:"valueB"]

		when: "using the dynamic method"
			service.getTwitterResourceWithQuerystringParams(theToken, theUrl, theQuerystringParams, theExtraHeaders)

		then: "the dynamic method is correctly identified"
			notThrown MissingMethodException
		and: "the service delegates correctly"
			1 * oauthResourceService.accessResource(theProviderService, theToken, { resourceAccessor ->
				resourceAccessor.verb               == Verb.POST
				resourceAccessor.url                == theUrl
				resourceAccessor.headers            == theExtraHeaders
				resourceAccessor.querystringParams  == theQuerystringParams
			} as ResourceAccessor)

	}

    // TODO: What if a dynamic provider requestToken, accessToken etc provider name is not known?

    class InvalidProviderApi {

    }

    class CustomProviderApi implements BaseApi {

        OAuthService createService(OAuthConfig oAuthConfig) {
            return null
        }

    }

}

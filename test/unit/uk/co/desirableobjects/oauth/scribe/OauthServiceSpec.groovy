package uk.co.desirableobjects.oauth.scribe

import grails.plugin.spock.UnitSpec
import uk.co.desirableobjects.oauth.scribe.exception.InvalidOauthProviderException
import org.scribe.builder.api.Api
import org.scribe.oauth.OAuthService
import org.scribe.model.OAuthConfig
import org.scribe.builder.api.TwitterApi

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import org.scribe.builder.ServiceBuilder
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.InvalidUriException
import org.springframework.beans.factory.BeanCreationException
import grails.test.mixin.TestFor

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
            1 == 1
            thrown IllegalStateException
    }

    def 'OAuthService can handle multiple providers'() {

        given:
            OauthService service = new OauthService()
            service.grailsApplication = [config: [
                    oauth: [
                        providers: [
                            twitter: [
                                api: org.scribe.builder.api.TwitterApi,
                                key: "twitter",
                                secret: "identica" ],
                            facebook: [
                                api: org.scribe.builder.api.FacebookApi,
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
                                api: org.scribe.builder.api.TwitterApi,
                                key: "twitter",
                                secret: "identica" ],
                            facebook: [
                                api: org.scribe.builder.api.FacebookApi,
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
                                api: org.scribe.builder.api.TwitterApi,
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
                                api: org.scribe.builder.api.TwitterApi,
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
                                api: org.scribe.builder.api.TwitterApi,
                                key: key,
                                secret: secret ],
                            facebook: [
                                api: org.scribe.builder.api.FacebookApi,
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
                                api: org.scribe.builder.api.TwitterApi,
                                key: 'myKey',
                                secret: 'mySecret',
                                callback: 'http://example.com:1234/url',
                                signatureType: org.scribe.model.SignatureType.QueryString ] ]]]]

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
                                api: org.scribe.builder.api.TwitterApi,
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
            service.grailsApplication = [config: [
                    oauth: [
                        providers: [
                            twitter: [
                                api: org.scribe.builder.api.TwitterApi,
                                key: 'myKey',
                                secret: 'mySecret' ]],
                        connectTimeout: 5000,
                        receiveTimeout: 5000 ]]]

        when:
            service.afterPropertiesSet()

        then:
            service.connectTimeout == 5000
            service.receiveTimeout == 5000

    }


    def 'if connection and recieve timeouts are not set, they are defaulted to thirty seconds'() {

        given:
            OauthService service = new OauthService()
            service.grailsApplication = [config: [
                    oauth: [
                        providers: [
                            twitter: [
                                api: org.scribe.builder.api.TwitterApi,
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
            'org.scribe.builder.api.TwitterApi'   | SupportedOauthVersion.ONE
            'org.scribe.builder.api.FacebookApi'  | SupportedOauthVersion.TWO

    }

    // TODO: What if a dynamic provider requestToken, accessToken etc provider name is not known?

    class InvalidProviderApi {

    }

    class CustomProviderApi implements Api {

        OAuthService createService(OAuthConfig oAuthConfig) {
            return null
        }
        
    }

}

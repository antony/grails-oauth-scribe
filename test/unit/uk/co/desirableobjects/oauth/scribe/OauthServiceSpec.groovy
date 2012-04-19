package uk.co.desirableobjects.oauth.scribe

import grails.plugin.spock.UnitSpec
import uk.co.desirableobjects.oauth.scribe.exception.InvalidOauthProviderException
import org.scribe.builder.api.Api
import org.scribe.oauth.OAuthService
import org.scribe.model.OAuthConfig
import org.scribe.builder.api.TwitterApi

import spock.lang.Unroll
import org.scribe.model.Token
import org.gmock.WithGMock
import org.scribe.builder.ServiceBuilder

class OauthServiceSpec extends UnitSpec {

    def 'Configuration is missing'() {

        when:
            new OauthService()

        then:
            thrown(IllegalStateException)

    }

    def 'OAuthService can handle multiple providers'() {

        given:
            mockConfig """
                import org.scribe.builder.api.TwitterApi
                import org.scribe.builder.api.FacebookApi

                oauth {
                    providers {
                        twitter {
                            api = TwitterApi
                            key = 'twitter'
                            secret = 'identica'
                        }
                        facebook {
                            api = FacebookApi
                            key = 'zuckerberg'
                            secret = 'brothers'
                        }
                    }
                }
            """

        expect:
            OauthService service = new OauthService()
            service.services.size() == 2

    }

    def 'OAuthService lower-cases provider names'() {

        given:
            mockConfig """
                import org.scribe.builder.api.TwitterApi
                import org.scribe.builder.api.FacebookApi

                oauth {
                    providers {
                        twitter {
                            api = TwitterApi
                            key = 'twitter'
                            secret = 'identica'
                        }
                        FaceBook {
                            api = FacebookApi
                            key = 'zuckerberg'
                            secret = 'brothers'
                        }
                    }
                }
            """

        expect:
            OauthService service = new OauthService()
            service.services.keySet() == ['twitter', 'facebook'] as Set<String>

    }

    @Unroll({"Configuration contains ${provider} provider"})
    def 'Configuration contains valid provider'() {

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

        expect:
            new OauthService()

        where:
            provider << [TwitterApi, CustomProviderApi]

    }

    @Unroll({"Configuration enables debug support when debug = ${debug}"})
    def 'Configuration enables debugging support'() {

        given:

            boolean debugEnabled = false
            ServiceBuilder.metaClass.debug = { debugEnabled = true }

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
                    debug = ${debug}
                }
            """

        and:
            new OauthService()

        expect:
            debugEnabled == debug

        where:
            debug << [false, true]

    }

    @Unroll({"Configuration includes or excludes a scope when scope is = ${scope}"})
    def 'Configuration includes scope'() {

        given:

            String providedScope = null
            ServiceBuilder.metaClass.scope = { String passedScope -> providedScope = passedScope }

            mockConfig """
                import org.scribe.builder.api.TwitterApi

                oauth {
                    providers {
                        twitter {
                            api = TwitterApi
                            key = 'myKey'
                            secret = 'mySecret'
                            ${scope ? "scope = 'testScope'" : ""}
                        }
                    }
                }
            """

        and:
            new OauthService()

        expect:
            providedScope == (scope ? 'testScope' : null)

        where:
            scope << [true, false]

    }


    void cleanup() {
        ServiceBuilder.metaClass = null
    }

    def 'Configuration contains a non-class as a provider'() {

            when:
                mockConfig """

                    oauth {
                        providers {
                            invalid {
                                api = 'some custom string'
                                key = 'myKey'
                                secret = 'mySecret'
                            }
                        }
                    }
                """

            and:
                new OauthService()

            then:
                thrown(InvalidOauthProviderException)

    }

    def 'Configuration contains a non-implementing class as a provider'() {

            when:
                mockConfig """
                    import uk.co.desirableobjects.oauth.scribe.OauthServiceSpec.InvalidProviderApi

                    oauth {
                        providers {
                            invalid {
                                api = InvalidProviderApi
                                key = 'myKey'
                                secret = 'mySecret'
                            }
                        }
                    }
                """

            and:
                new OauthService()

            then:
                thrown(InvalidOauthProviderException)

    }

    @Unroll({"Configuration is provided with invalid key (${key}) or secret (${secret})"})
    def 'Configuration is missing keys and or secrets'() {


        given:

            mockConfig """
                import org.scribe.builder.api.*

                oauth {
                    providers {
                        twitter {
                            api = TwitterApi
                            key = ${key}
                            secret = ${secret}
                        }
                        facebook {
                            api = FacebookApi
                            key = 'facebook-key'
                            secret = 'facebook-secret'
                        }
                    }
                }
            """

        when:

            new OauthService()

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

        when:
            mockConfig """
                    import org.scribe.builder.api.TwitterApi
                    import org.scribe.model.SignatureType

                    oauth {
                        providers {
                            twitter {
                                api = TwitterApi
                                key = 'myKey'
                                secret = 'mySecret'
                                callback = 'http://example.com:1234/url'
                                signatureType = SignatureType.QueryString
                            }
                        }
                    }
                """

        then:

            new OauthService()

    }

    def 'configuration contains a successUri and a failureUri'() {

        given:

            mockConfig '''
                import org.scribe.builder.api.TwitterApi
                import org.scribe.model.SignatureType

                oauth {
                    providers {
                        twitter {
                            api = TwitterApi
                            key = 'myKey'
                            secret = 'mySecret'
                            successUri = '/coffee/tea'
                            failureUri = '/cola/pepsi'
                        }
                    }
                }
            '''

        when:

            OauthService service = new OauthService()

        then:

            service.services.twitter.successUri == '/coffee/tea'
            service.services.twitter.failureUri == '/cola/pepsi'

    }

    def 'configuration can set socket and receive timeouts'() {

        given:

            mockConfig '''
                import org.scribe.builder.api.TwitterApi
                import org.scribe.model.SignatureType

                oauth {
                    providers {
                        twitter {
                            api = TwitterApi
                            key = 'myKey'
                            secret = 'mySecret'
                        }
                    }
                    connectTimeout = 5000
                    receiveTimeout = 5000
                }
            '''

        when:

            OauthService service = new OauthService()

        then:

            service.connectTimeout == 5000
            service.receiveTimeout == 5000

    }


    def 'if connection and recieve timeouts are not set, they are defaulted to thirty seconds'() {

        given:
            mockConfig '''
                import org.scribe.builder.api.TwitterApi
                import org.scribe.model.SignatureType

                oauth {
                    providers {
                        twitter {
                            api = TwitterApi
                            key = 'myKey'
                            secret = 'mySecret'
                            successUri = '/coffee/tea'
                            failureUri = '/cola/pepsi'
                        }
                    }
                }
            '''

        when:
            OauthService service = new OauthService()

        then:
            service.connectTimeout == 30000
            service.receiveTimeout == 30000

    }

    @Unroll({"Service returns correct API version when given ${apiClass}"})
    def 'Service returns correct API version'() {

        given:

            mockConfig """
                import org.scribe.model.SignatureType

                oauth {
                    providers {
                        dynamic {
                            api = ${apiClass}
                            key = 'myKey'
                            secret = 'mySecret'
                        }
                    }
                }
            """

        when:

            OauthService service = new OauthService()

        then:

            service.services.dynamic.oauthVersion == apiVersion

        where:
            apiClass                                                               | apiVersion
            'uk.co.desirableobjects.oauth.scribe.test.Test10aApiImplementation'    | SupportedOauthVersion.ONE
            'org.scribe.builder.api.FacebookApi'                                   | SupportedOauthVersion.TWO

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

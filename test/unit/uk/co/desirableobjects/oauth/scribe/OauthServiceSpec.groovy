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

@Mixin(GMockAddon)
class OauthServiceSpec extends UnitSpec {

    def 'Configuration is missing'() {

        when:
            new OauthService()

        then:
            thrown(IllegalStateException)

    }

    @Unroll('Configuration contains #provider provider')
    def 'Configuration contains valid provider'() {

        given:
            mockConfig """
                import org.scribe.builder.api.TwitterApi

                oauth {
                    provider = TwitterApi
                    key = 'myKey'
                    secret = 'mySecret'
                }
            """

        expect:
            new OauthService()

        where:
            provider << [TwitterApi, CustomProviderApi]

    }

    def 'Configuration contains a non-class as a provider'() {

            when:
                mockConfig """
                    import uk.co.desirableobjects.oauth.scribe.OauthServiceSpec.InvalidProviderApi

                    oauth {
                        provider = 'some custom string'
                        key = 'myKey'
                        secret = 'mySecret'
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
                        provider = InvalidProviderApi
                        key = 'myKey'
                        secret = 'mySecret'
                    }
                """

            and:
                new OauthService()

            then:
                thrown(InvalidOauthProviderException)

    }

    @Unroll("Configuration is provided with invalid key (#key) or secret (#secret)")
    def 'Configuration is missing keys and or secrets'() {


        given:

            mockConfig """
                import org.scribe.builder.api.TwitterApi

                oauth {
                    provider = TwitterApi
                    key = ${key}
                    secret = ${secret}
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

    def 'callback URL is supported but optional'() {

        when:
            mockConfig """
                    import org.scribe.builder.api.TwitterApi

                    oauth {
                        provider = TwitterApi
                        key = 'myKey'
                        secret = 'mySecret'
                        callback = 'http://example.com:1234/url'
                    }
                """

        then:

            new OauthService()

    }


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
            OauthService service = new OauthService()

            service.service = mock(OAuthService)
            service.service.getRequestToken().returns(new Token('a', 'b', 'c'))

        when:
            Token token
            simulate {
                token = service.requestToken
            }

        then:
            token.rawResponse == 'c'

    }


    class InvalidProviderApi {

    }

    class CustomProviderApi implements Api {

        OAuthService createService(OAuthConfig oAuthConfig) {
            return null
        }
        
    }

}

package uk.co.desirableobjects.oauth.scribe

import grails.plugin.spock.UnitSpec
import uk.co.desirableobjects.oauth.scribe.exception.InvalidOauthProviderException
import org.scribe.builder.api.Api
import org.scribe.oauth.OAuthService
import org.scribe.model.OAuthConfig
import org.scribe.builder.api.TwitterApi

import spock.lang.Unroll

class OauthServiceSpec extends UnitSpec {

    @Unroll('Configuration contains #provider provider')
    def 'Configuration contains valid provider'() {

        given:
            mockConfig """
                import org.scribe.builder.api.TwitterApi

                oauth {
                    provider = TwitterApi
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
                    }
                """

            and:
                new OauthService()

            then:
                thrown(InvalidOauthProviderException)

    }

    class InvalidProviderApi {

    }

    class CustomProviderApi implements Api {

        OAuthService createService(OAuthConfig oAuthConfig) {
            return null
        }
        
    }

}

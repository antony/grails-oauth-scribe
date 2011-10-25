package uk.co.desirableobjects.oauth.scribe

import grails.plugin.spock.TagLibSpec
import org.gmock.WithGMock
import org.scribe.model.Token
import org.scribe.model.Verifier

@Mixin(GMockAddon)
class OauthTagLibSpec extends TagLibSpec {

    def 'an oauth link tag can be rendered'() {

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
            tagLib.oauthService = mock(OauthService)
            tagLib.oauthService.requestToken.returns(requestToken)
            tagLib.oauthService.getAuthorizationUrl(requestToken).returns('http://authorisation.url/auth')

//        and:
//            Verifier verifier = new Verifier('http://authorisation.url/auth')
//            Token accessToken = new Token('d', 'e', 'f')
//            tagLib.oauthService.getAuthorizationUrl(requestToken).returns(verifier.value)
//            tagLib.oauthService.getAccessToken(requestToken, verifier).returns(accessToken)

        when:

            simulate {
                tagLib.link([], { 'Click here to authorise' } )
            }

        then:

            tagLib.out.toString() == '<a href="http://authorisation.url/auth">Click here to authorise</a>'

    }

}

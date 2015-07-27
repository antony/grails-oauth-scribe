package uk.co.desirableobjects.oauth.scribe

import grails.test.mixin.TestFor
import org.grails.taglib.GrailsTagException
import org.scribe.model.Token
import spock.lang.Specification

@TestFor(OauthTagLib)
class OauthTagLibSpec extends Specification {

    Map assertions = [:]

    void setup() {

        assertions.clear()

    }

    def 'an oauth link tag can be rendered'() {

        given:

            OauthTagLib.metaClass.g.link = { attrs, body ->
                assertions.put(body(), 'Click here to authorise')
            }

        when:

            tagLib.connect([provider:'twitter'], { 'Click here to authorise' } )

        then:

            asExpectations()

    }

    def 'an oauth connected tag body is rendered when token in session'() {

        given:

            tagLib.oauthService = new OauthService()
            tagLib.oauthService.findSessionKeyForAccessToken('twitter') >> { return 'twitter:oasAccessToken' }

        and: 

            tagLib.session['twitter:oasAccessToken'] = new Token('a', 'b', 'c')

        when:

            def output = tagLib.connected([provider:'twitter'], { 'Connected content' } )

        then:

            output == 'Connected content'

    }

    def 'an oauth connected tag body is NOT rendered when token does not exist in session'() {

        given:

            tagLib.oauthService = new OauthService()
            tagLib.oauthService.findSessionKeyForAccessToken('twitter') >> { return 'twitter:oasAccessToken' }

        and: 

            tagLib.session['twitter:oasAccessToken'] = null

        when:

            def output = tagLib.connected([provider:'twitter'], { 'Connected content' } )

        then:

            output == ''

    }

    def 'an oauth disconnected tag body is NOT rendered when token in session'() {

        given:

            tagLib.oauthService = new OauthService()
            tagLib.oauthService.findSessionKeyForAccessToken('twitter') >> { return 'twitter:oasAccessToken' }

        and: 

            tagLib.session['twitter:oasAccessToken'] = new Token('a', 'b', 'c')

        when:

            def output = tagLib.disconnected([provider:'twitter'], { 'Disconnected content' } )

        then:

            output == ''

    }

    def 'an oauth disconnected tag body is rendered when token does not exist in session'() {

        given:

            tagLib.oauthService = new OauthService()
            tagLib.oauthService.findSessionKeyForAccessToken('twitter') >> { return 'twitter:oasAccessToken' }

        and: 

            tagLib.session['twitter:oasAccessToken'] = null

        when:

            def output = tagLib.disconnected([provider:'twitter'], { 'Disconnected content' } )

        then:

            output == 'Disconnected content'

    }

    def 'an oauth link tag fails if provider is not specified'() {

        when:

            tagLib.connect([:], { 'Click here to authorise' } )

        then:

            thrown GrailsTagException

    }

    def 'an oauth connected tag fails if provider is not specified'() {

        when:

            tagLib.connected([:], { 'Connected content' } )

        then:

            thrown GrailsTagException

    }

    def 'an oauth disconnected tag fails if provider is not specified'() {

        when:

            tagLib.disconnected([:], { 'Disconnected content' } )

        then:

            thrown GrailsTagException

    }

    private boolean asExpectations() {

        assertions.each { actual, expected ->

            if (actual != expected) {
                throw new RuntimeException('Expectation failed: '+actual+" != "+expected)
            }

        }

        return true

    }

    def 'an oauth link tag renders its arguments as passed'() {

        given:

            OauthTagLib.metaClass.g.link = { attrs, body ->
                assertions.put(attrs.'class', 'ftw')
            }

        when:

            tagLib.connect([provider: 'twitter', 'class':'ftw'], { 'Click here to authorise' } )

        then:

            asExpectations()

    }

}

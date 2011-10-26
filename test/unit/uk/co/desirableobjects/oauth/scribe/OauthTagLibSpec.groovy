package uk.co.desirableobjects.oauth.scribe

import grails.plugin.spock.TagLibSpec
import org.gmock.WithGMock
import org.scribe.model.Token
import org.scribe.model.Verifier
import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib

@Mixin(GMockAddon)
class OauthTagLibSpec extends TagLibSpec {

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

            tagLib.connect([:], { 'Click here to authorise' } )

        then:

            asExpectations()

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

            tagLib.connect(['class':'ftw'], { 'Click here to authorise' } )

        then:

            asExpectations()

    }

}

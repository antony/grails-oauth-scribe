package uk.co.desirableobjects.oauth.scribe

import grails.plugin.spock.TagLibSpec
import org.gmock.WithGMock
import org.scribe.model.Token
import org.scribe.model.Verifier
import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib
import grails.test.mixin.TestFor
import spock.lang.Specification
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException

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

    def 'an oauth link tag fails if provider is not specified'() {

        when:

            tagLib.connect([:], { 'Click here to authorise' } )

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

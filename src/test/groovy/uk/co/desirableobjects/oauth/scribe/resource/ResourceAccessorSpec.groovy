package uk.co.desirableobjects.oauth.scribe.resource

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
class ResourceAccessorSpec extends Specification {

    def 'content length is correctly calculated'() {

        given:
            ResourceAccessor resourceAccessor = new ResourceAccessor(payload: payload)

        expect:
            resourceAccessor.headers['Content-Length'] == contentLength
            resourceAccessor.payload == payload

        where:
            payload         | contentLength
            [13, 10]        | '2'
            'Hello'.bytes   | '5'


    }

    def 'User can add but not set a header'() {

        when:
            ResourceAccessor resourceAccessor = new ResourceAccessor()
            resourceAccessor.addHeader('a', 'b')

        then:
            resourceAccessor.headers == [a: 'b']

        when:
            resourceAccessor.headers = [:]

        then:
            IllegalAccessException iae = thrown IllegalAccessException
            iae.message == "Setting headers would overwrite auto-generated header data. Use addHeader to add a new or override an existing header"

    }

}

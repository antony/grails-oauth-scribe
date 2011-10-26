package uk.co.desirableobjects.oauth.scribe

import grails.plugin.spock.TagLibSpec
import org.gmock.WithGMock
import org.scribe.model.Token
import org.scribe.model.Verifier
import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib

@Mixin(GMockAddon)
class OauthTagLibSpec extends TagLibSpec {

    void setup() {

        OauthTagLib.metaClass.link = { attrs, body -> return "<a href=\"http://local.host/${attrs.url.controller}/${attrs.url.action}\">"+body()+"</a>" }

    }

    def 'an oauth link tag can be rendered'() {

        when:

            tagLib.connect([], { 'Click here to authorise' } )

        then:

            tagLib.out.toString() == '<a href="http://local.host/oauth/authenticate">Click here to authorise</a>'

    }

}

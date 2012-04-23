package uk.co.desirableobjects.oauth.scribe

import uk.co.desirableobjects.oauth.scribe.util.DynamicMethods
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException

class OauthTagLib {

    static namespace = 'oauth'

    def connect = { attrs, body ->
        
        String provider = attrs.provider

        if (!provider) {
            throw new GrailsTagException('No provider specified for <oauth:connect /> tag. Try <oauth:connect provider="your-provider-name" />')
        }

        Map a = attrs+[url:[controller:'oauth', action:'authenticate', params:[provider:provider]]]
        out << g.link(a, body)

    }

}

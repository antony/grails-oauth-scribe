package uk.co.desirableobjects.oauth.scribe

import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import org.scribe.model.Token

class OauthTagLib {

    static namespace = 'oauth'

    OauthService oauthService

    def connect = { attrs, body ->

        String provider = attrs.provider

        if (!provider) {
            throw new GrailsTagException('No provider specified for <oauth:connect /> tag. Try <oauth:connect provider="your-provider-name" />')
        }

        Map a = attrs+[url:[controller:'oauth', action:'authenticate', params:[provider:provider]]]
        out << g.link(a, body)

    }

    def connected = { attrs, body ->

    	String provider = attrs.provider

        if (!provider) {
            throw new GrailsTagException('No provider specified for <oauth:connected /> tag. Try <oauth:connected provider="your-provider-name" />')
        }

        String sessionKey = oauthService.findSessionKeyForAccessToken(provider)
        Token oauthToken = session[sessionKey]

        if (oauthToken) {
            out << body()
        }

     }

     def disconnected = { attrs, body ->

     	String provider = attrs.provider

        if (!provider) {
            throw new GrailsTagException('No provider specified for <oauth:disconnected /> tag. Try <oauth:disconnected provider="your-provider-name" />')
        }

        String sessionKey = oauthService.findSessionKeyForAccessToken(provider)
        Token oauthToken = session[sessionKey]

        if (!oauthToken) {

            out << body()
        }

     }

}

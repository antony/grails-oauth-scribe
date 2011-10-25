package uk.co.desirableobjects.oauth.scribe

import org.scribe.model.Token

class OauthTagLib {

    static namespace = 'oauth'

    OauthService oauthService

    def link = { attrs, body ->

        Token requestToken = oauthService.requestToken
        String url = oauthService.getAuthorizationUrl(requestToken)

        out << String.format('<a href="%s">%s</a>', url, body())

    }

}

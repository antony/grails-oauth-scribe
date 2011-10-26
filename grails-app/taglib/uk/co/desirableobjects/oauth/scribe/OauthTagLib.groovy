package uk.co.desirableobjects.oauth.scribe

class OauthTagLib {

    static namespace = 'oauth'

    def connect = { attrs, body ->

        out << g.link(url:[controller:'oauth', action:'authenticate'], body)

    }

}

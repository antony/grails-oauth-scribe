package uk.co.desirableobjects.oauth.scribe

class OauthTagLib {

    static namespace = 'oauth'

    def connect = { attrs, body ->

        Map a = attrs+[url:[controller:'oauth', action:'authenticate']]
        out << g.link(a, body)

    }

}

package grails.plugins.uk.co.desirableobjects.oauth.scribe.exception

class MissingRequestTokenException extends RuntimeException {

    MissingRequestTokenException(String providerName) {

        super("We couldn't find a request token for ${providerName} in the session. A common cause of this is that you have been given a new session by the servlet container because your callback domain is different to the domain you are authenticating from. Check that the domain name in the URL bar of your browser matches the domain name of your callback URL")

    }

}

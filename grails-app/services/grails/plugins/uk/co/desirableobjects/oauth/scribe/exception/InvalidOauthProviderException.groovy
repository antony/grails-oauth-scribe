package grails.plugins.uk.co.desirableobjects.oauth.scribe.exception

class InvalidOauthProviderException extends RuntimeException {

    InvalidOauthProviderException(String message) {
        super(message)
    }

    InvalidOauthProviderException(String message, Throwable t) {
        super(message)
    }

}

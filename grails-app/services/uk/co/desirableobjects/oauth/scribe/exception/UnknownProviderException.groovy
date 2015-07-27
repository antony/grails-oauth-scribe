package uk.co.desirableobjects.oauth.scribe.exception

class UnknownProviderException extends RuntimeException {

    UnknownProviderException(String requestedProvider) {
        super("Unknown provider ${requestedProvider}, check your configuration.")
    }

}

package uk.co.desirableobjects.oauth.scribe.exception

class InvalidProviderClassException extends RuntimeException {
    InvalidProviderClassException(String name, Object clazz) {
        super("${clazz} configured as an API for ${name} does not appear to be a valid Class. It should be a class extending from the org.scribe.builder.Api class")
    }
}
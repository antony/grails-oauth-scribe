package uk.co.desirableobjects.oauth.scribe

import org.codehaus.groovy.grails.commons.ConfigurationHolder as CH
import org.scribe.builder.api.Api
import uk.co.desirableobjects.oauth.scribe.exception.InvalidOauthProviderException
import org.codehaus.groovy.runtime.typehandling.GroovyCastException

class OauthService {

    OauthService() {

        Api provider

        try {
            provider = CH.config.oauth.provider
        } catch (GroovyCastException gce) {
            throw new InvalidOauthProviderException("${CH.config.oauth.provider} is not a Class or does not implement the Api interface" as String)
        }

    }

}

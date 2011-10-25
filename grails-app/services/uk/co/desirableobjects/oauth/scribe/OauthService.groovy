package uk.co.desirableobjects.oauth.scribe

import org.codehaus.groovy.grails.commons.ConfigurationHolder as CH
import org.scribe.builder.api.Api
import uk.co.desirableobjects.oauth.scribe.exception.InvalidOauthProviderException
import org.codehaus.groovy.runtime.typehandling.GroovyCastException
import org.scribe.builder.ServiceBuilder
import org.scribe.oauth.OAuthService
import org.scribe.exceptions.OAuthException

class OauthService {

    private OAuthService service

    OauthService() {

        checkConfigurationPresent()

        Class<? extends Api> provider

        try {
            provider = CH.config.oauth.provider

            service = new ServiceBuilder()
                           .provider(provider)
                           .apiKey(CH.config?.oauth.key)
                           .apiSecret(CH.config?.oauth.key)
                           .build();

        } catch (GroovyCastException gce) {
            throw new InvalidOauthProviderException("${CH.config.oauth.provider} is not a Class" as String)
        } catch (OAuthException oae) {
            throw new InvalidOauthProviderException("${CH.config.oauth.provider} does not implement the Api interface" as String, oae)
        }

    }

    private void checkConfigurationPresent() {
        if (!CH.config?.oauth) {
            throw new IllegalStateException('No oauth configuration found. Please configure the oauth scribe plugin')
        }
        if (!CH.config?.oauth.key || !CH.config?.oauth.secret) {
            throw new IllegalStateException("Missing oauth secret or key (or both!) in configuration.")
        }
    }

}

package uk.co.desirableobjects.oauth.scribe

import org.codehaus.groovy.grails.commons.ConfigurationHolder as CH
import org.scribe.builder.api.Api
import uk.co.desirableobjects.oauth.scribe.exception.InvalidOauthProviderException
import org.codehaus.groovy.runtime.typehandling.GroovyCastException
import org.scribe.builder.ServiceBuilder
import org.scribe.oauth.OAuthService
import org.scribe.exceptions.OAuthException
import org.scribe.model.Token
import org.scribe.model.Verifier
import org.scribe.model.Verb
import org.scribe.model.Response

class OauthService {

    protected OAuthService service

    OACommunicationService oaCommunicationService

    OauthService() {

        checkConfigurationPresent()

        try {

            service = buildService()

        } catch (GroovyCastException gce) {
            throw new InvalidOauthProviderException("${CH.config.oauth.provider} is not a Class" as String)
        } catch (OAuthException oae) {
            throw new InvalidOauthProviderException("${CH.config.oauth.provider} does not implement the Api interface" as String, oae)
        }

    }

    private OAuthService buildService() {

        Class<? extends Api> provider = CH.config.oauth.provider
        String callback = CH.config.oauth?.callback

        ServiceBuilder serviceBuilder = new ServiceBuilder()
        .provider(provider)
        .apiKey(CH.config?.oauth.key)
        .apiSecret(CH.config?.oauth.key)

        if (CH.config.oauth?.callback) {
            serviceBuilder.callback(callback)
        }

        service = serviceBuilder.build()
    }

    private void checkConfigurationPresent() {
        if (!CH.config?.oauth) {
            throw new IllegalStateException('No oauth configuration found. Please configure the oauth scribe plugin')
        }
        if (!CH.config?.oauth.key || !CH.config?.oauth.secret) {
            throw new IllegalStateException("Missing oauth secret or key (or both!) in configuration.")
        }
    }

    Token getRequestToken() {

        return service.requestToken

    }

    String getAuthorizationUrl(Token token) {

        return service.getAuthorizationUrl(token)

    }

    Token getAccessToken(Token token, Verifier verifier) {

        return service.getAccessToken(token, verifier)

    }

    def methodMissing(String name, args) {

           if( name ==~ /^.*Resource/) {

              def m = name =~ /^(.*)Resource/
              Verb verb = Verb.values().find { Verb verb -> verb.name() == (String) m[0][1] }
              return accessResource(service, args[0] as Token, verb, args[1] as String)

           } else {

               throw new MissingMethodException(name, this.class, args)


           }
    }

    private Response accessResource(OAuthService oaService, Token accessToken, Verb verb, String url) {

        println "calling"

        return oaCommunicationService.accessResource(service, accessToken, verb, url)
        
    }

}

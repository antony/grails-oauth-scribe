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
import org.scribe.model.SignatureType

class OauthService {

    protected OAuthService service
    OACommunicationService oaCommunicationService

    public static final String REQUEST_TOKEN_SESSION_KEY = 'oasRequestToken'
    public static final String ACCESS_TOKEN_SESSION_KEY = 'oasAccessToken'

    String successUri
    String failureUri

    OauthService() {

        checkConfigurationPresent()

        try {

            service = buildService()
            this.successUri = CH.config.oauth.successUri
            this.failureUri = CH.config.oauth.failureUri

        } catch (GroovyCastException gce) {
            throw new InvalidOauthProviderException("${CH.config.oauth.provider} is not a Class" as String)
        } catch (OAuthException oae) {
            throw new InvalidOauthProviderException("${CH.config.oauth.provider} does not implement the Api interface" as String, oae)
        }

    }

    private OAuthService buildService() {

        ConfigObject oaConfig = CH.config.oauth

        Class provider = oaConfig.provider
        String callback = oaConfig.containsKey('callback') ? oaConfig.callback : null
        SignatureType signatureType = CH.config.oauth.containsKey('signatureType') ? CH.config.oauth?.signatureType : null

        ServiceBuilder serviceBuilder = new ServiceBuilder()
        .provider(provider)
        .apiKey(CH.config?.oauth.key)
        .apiSecret(CH.config?.oauth.secret)

        if (callback) {
            serviceBuilder.callback(callback)
        }

        if (signatureType) {
            serviceBuilder.signatureType(signatureType)
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
              String verb = (String) m[0][1]

              if (Verb.values()*.name().find { it == verb.toUpperCase() } ) {
                  return accessResource(args[0] as Token, verb, args[1] as String)
              }

       }

       throw new MissingMethodException(name, this.class, args)

    }

    Response accessResource(Token accessToken, String verbName, String url) {

        Verb verb = Verb.valueOf(verbName.toUpperCase())
        return oaCommunicationService.accessResource(service, accessToken, verb, url)
        
    }

}

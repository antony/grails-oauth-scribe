package uk.co.desirableobjects.oauth.scribe

import org.codehaus.groovy.grails.commons.ConfigurationHolder as CH

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

    private static final int THIRTY_SECONDS = 30000
    protected OAuthService service
    OauthResourceService oauthResourceService

    public static final String REQUEST_TOKEN_SESSION_KEY = 'oasRequestToken'
    public static final String ACCESS_TOKEN_SESSION_KEY = 'oasAccessToken'

    String successUri
    String failureUri
    private int connectTimeout
    private int receiveTimeout

    OauthService() {

        ConfigObject conf = fetchConfig()

        try {

            service = buildService(conf)

        } catch (GroovyCastException gce) {
            throw new InvalidOauthProviderException("${CH.config.oauth.provider} is not a Class" as String)
        } catch (OAuthException oae) {
            throw new InvalidOauthProviderException("${CH.config.oauth.provider} does not implement the Api interface" as String, oae)
        }

        configureProvider(conf)

    }

    private void configureProvider(ConfigObject conf) {

        successUri = conf.successUri
        failureUri = conf.failureUri
        connectTimeout = conf.containsKey('connectTimeout') ? conf.connectTimeout : THIRTY_SECONDS
        receiveTimeout = conf.containsKey('receiveTimeout') ? conf.receiveTimeout : THIRTY_SECONDS

    }

    private OAuthService buildService(ConfigObject conf) {

        Class provider = conf.provider
        String callback = conf.containsKey('callback') ? conf.callback : null
        SignatureType signatureType = conf.containsKey('signatureType') ? conf.signatureType : null

        ServiceBuilder serviceBuilder = new ServiceBuilder()
        .provider(provider)
        .apiKey(conf.key as String)
        .apiSecret(conf.secret as String)

        if (callback) {
            serviceBuilder.callback(callback)
        }

        if (signatureType) {
            serviceBuilder.signatureType(signatureType)
        }

        service = serviceBuilder.build()
    }

    private ConfigObject fetchConfig() {

        if (!CH.config?.oauth) {
            throw new IllegalStateException('No oauth configuration found. Please configure the oauth scribe plugin')
        }

        ConfigObject conf = CH.config.oauth

        if (!conf.key || !conf.secret) {
            throw new IllegalStateException("Missing oauth secret or key (or both!) in configuration.")
        }

        return conf
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
                  return this.accessResource(args[0] as Token, verb, args[1] as String)
              }

       }

       throw new MissingMethodException(name, this.class, args)

    }

    Response accessResource(Token accessToken, String verbName, String url) {

        Verb verb = Verb.valueOf(verbName.toUpperCase())
        return oauthResourceService.accessResource(service, accessToken, verb, url, connectTimeout, receiveTimeout)
        
    }

}

package uk.co.desirableobjects.oauth.scribe

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
import uk.co.desirableobjects.oauth.scribe.exception.UnknownProviderException
import uk.co.desirableobjects.oauth.scribe.resource.ResourceAccessor
import uk.co.desirableobjects.oauth.scribe.util.DynamicMethods
import org.springframework.beans.factory.InitializingBean
import uk.co.desirableobjects.oauth.scribe.exception.InvalidProviderClassException
import org.scribe.model.OAuthRequest
import java.util.concurrent.TimeUnit

class OauthService implements InitializingBean {

    static transactional = false

    private static final int THIRTY_SECONDS = 30000
    Map<String, OauthProvider> services = [:]
    OauthResourceService oauthResourceService
    def grailsApplication

    private int connectTimeout
    private int receiveTimeout

    String findSessionKeyForRequestToken(String providerName) {
        return "${providerName}:oasRequestToken"
    }

    String findSessionKeyForAccessToken(String providerName) {
        return "${providerName}:oasAccessToken"
    }

    void afterPropertiesSet() {

        Map conf = fetchConfig()

        try {

            buildService(conf)

        } catch (InvalidProviderClassException ipce) {
            throw new InvalidOauthProviderException(ipce.message)
        } catch (OAuthException oae) {
            throw new InvalidOauthProviderException(oae.message, oae)
        }

        configureTimeouts(conf)

    }

    private void configureTimeouts(Map conf) {

        connectTimeout = conf.containsKey('connectTimeout') ? conf.connectTimeout : THIRTY_SECONDS
        receiveTimeout = conf.containsKey('receiveTimeout') ? conf.receiveTimeout : THIRTY_SECONDS

    }

    private void buildService(Map conf) {

        boolean debug = (conf.debug) ?: false

        conf.providers.each { configuration ->

                verifyConfiguration(configuration)

                String name = configuration.key.toString().toLowerCase()
                LinkedHashMap providerConfig = configuration.value

                Class api
                try {
                    api = providerConfig.api
                } catch (GroovyCastException gce) {
                    throw new InvalidProviderClassException(name, providerConfig.api)
                }

                String callback = providerConfig.containsKey('callback') ? providerConfig.callback : null
                SignatureType signatureType = providerConfig.containsKey('signatureType') ? providerConfig.signatureType : null
                String scope = providerConfig.containsKey('scope') ? providerConfig.scope : null

                ServiceBuilder serviceBuilder = new ServiceBuilder()
                        .provider(api)
                        .apiKey(providerConfig.key as String)
                        .apiSecret(providerConfig.secret as String)

                if (callback) {
                    serviceBuilder.callback(callback)
                }

                if (signatureType) {
                    serviceBuilder.signatureType(signatureType)
                }

                if (scope) {
                    serviceBuilder.scope(scope)
                }

                if (debug) {
                    serviceBuilder.debug()
                }

                OauthProvider provider = new OauthProvider(
                    service: serviceBuilder.build(),
                    successUri: providerConfig.successUri,
                    failureUri: providerConfig.failureUri
                )

                services.put(name, provider)

        }


    }

    private void verifyConfiguration(conf) {

        if (!conf.value.key || !conf.value.secret) {
            throw new IllegalStateException("Missing oauth secret or key (or both!) in configuration for ${conf.key}.")
        }

    }

    private Map fetchConfig() {

        Map conf = grailsApplication.config.oauth
        if (!conf) {
            throw new IllegalStateException('No oauth configuration found. Please configure the oauth scribe plugin')
        }

        return conf
    }

    private Token getRequestToken(String serviceName) {

        return findService(serviceName).getRequestToken()

    }

    String getAuthorizationUrl(String serviceName, Token token) {

        return findService(serviceName).getAuthorizationUrl(token)

    }

    Token getAccessToken(String serviceName, Token token, Verifier verifier) {

        return findService(serviceName).getAccessToken(token, verifier)

    }

    def methodMissing(String name, args) {

       if( name ==~ /^.*RequestToken/) {

           String provider = DynamicMethods.extractKeyword(name, /^get(.*)RequestToken/)
           return this.getRequestToken(provider)

       }

       if( name ==~ /^.*AuthorizationUrl/) {

            String provider = DynamicMethods.extractKeyword(name, /^get(.*)AuthorizationUrl/)
            return this.getAuthorizationUrl(provider, args[0])

       }

       if( name ==~ /^.*AccessToken/) {

            String provider = DynamicMethods.extractKeyword(name, /^get(.*)AccessToken/)
            return this.getAccessToken(provider, args[0], args[1])

       }

       if( name ==~ /^(get|put|post|delete|options|head).*Resource/) {

              def m = name =~ /^(get|put|post|delete|options|head)(.*)Resource/
              String verb = (String) m[0][1]
              String serviceName = (String) m[0][2].toString().toLowerCase()
              Verb actualVerb = Verb.valueOf(verb.toUpperCase())

               ResourceAccessor resourceAccessor = new ResourceAccessor(
                       verb: actualVerb,
                       url: args[1] as String,
                       bodyParameters: (args.length > 2) ? args[2] as Map : null
               )

               if (args.length > 3) {
                    resourceAccessor.addHeader('Content-Type', args[3] as String)
               }

               return oauthResourceService.accessResource(findService(serviceName), args[0] as Token, resourceAccessor)

       }

        if( name ==~ /^(get|put|post|delete|options|head).*ResourceWithPayload/) {

            def m = name =~ /^(get|put|post|delete|options|head)(.*)ResourceWithPayload/
            String verb = (String) m[0][1]
            String serviceName = (String) m[0][2].toString().toLowerCase()

            Verb actualVerb = Verb.valueOf(verb.toUpperCase())
            OAuthService service = findService(serviceName)

            ResourceAccessor resourceAccessor = new ResourceAccessor(
                    verb: actualVerb,
                    url: args[1] as String,
                    payload: (args[2] as String).bytes,
            )
            resourceAccessor.addHeader('Content-Type', args[3] as String)

            return oauthResourceService.accessResource(service, args[0] as Token, resourceAccessor)

        }
        throw new MissingMethodException(name, this.class, args)

    }

    protected OAuthService findService(String providerName) {

        return findProviderConfiguration(providerName).service
    }

    public OauthProvider findProviderConfiguration(String providerName) {

        if (!services.containsKey(providerName)) {
            throw new UnknownProviderException(providerName)
        }

        return services[providerName]

    }

}

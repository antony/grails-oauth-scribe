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
import uk.co.desirableobjects.oauth.scribe.exception.UnknownProviderException
import uk.co.desirableobjects.oauth.scribe.util.DynamicMethods

class OauthService {

    static def transactional = false

    private static final int THIRTY_SECONDS = 30000
    Map<String, OauthProvider> services = [:]
    OauthResourceService oauthResourceService

    private int connectTimeout
    private int receiveTimeout

    String findSessionKeyForRequestToken(String providerName) {
        return "${providerName}:oasRequestToken"
    }

    String findSessionKeyForAccessToken(String providerName) {
        return "${providerName}:oasAccessToken"
    }

    OauthService() {

        ConfigObject conf = fetchConfig()

        try {

            buildService(conf)

        } catch (GroovyCastException gce) {
            throw new InvalidOauthProviderException("${CH.config.oauth.provider} is not a Class" as String)
        } catch (OAuthException oae) {
            throw new InvalidOauthProviderException("${CH.config.oauth.provider} does not implement the Api interface" as String, oae)
        }

        configureTimeouts(conf)

    }

    private void configureTimeouts(ConfigObject conf) {

        connectTimeout = conf.containsKey('connectTimeout') ? conf.connectTimeout : THIRTY_SECONDS
        receiveTimeout = conf.containsKey('receiveTimeout') ? conf.receiveTimeout : THIRTY_SECONDS

    }

    private void buildService(ConfigObject conf) {

        boolean debug = (conf.debug) ?: false

        conf.providers.each { configuration ->

                verifyConfiguration(configuration)

                String name = configuration.key.toString().toLowerCase()
                LinkedHashMap providerConfig = configuration.value

                Class api = providerConfig.api
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

    private ConfigObject fetchConfig() {

        if (!CH.config?.oauth) {
            throw new IllegalStateException('No oauth configuration found. Please configure the oauth scribe plugin')
        }

        ConfigObject conf = CH.config.oauth

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

              if (Verb.values()*.name().find { it == verb.toUpperCase() } ) {
                  return this.accessResource(serviceName, args[0] as Token, verb, args[1] as String, args.length>2?args[2] as Map:null)

              }

       }

        if( name ==~ /^(get|put|post|delete|options|head).*ResourceWithPayload/) {

            def m = name =~ /^(get|put|post|delete|options|head)(.*)ResourceWithPayload/
            String verb = (String) m[0][1]
            String serviceName = (String) m[0][2].toString().toLowerCase()

            if (Verb.values()*.name().find { it == verb.toUpperCase() } ) {
                return this.accessResource(serviceName, args[0] as Token, verb, args[1] as String, args[2] as String,
                        args[3] as String)
            }

        }
        throw new MissingMethodException(name, this.class, args)

    }

    Response accessResource(String serviceName, Token accessToken, String verbName, String url, Map body) {

        Verb verb = Verb.valueOf(verbName.toUpperCase())
        return oauthResourceService.accessResource(findService(serviceName), accessToken, verb, url, body, connectTimeout, receiveTimeout)

    }

    Response accessResource(String serviceName, Token accessToken, String verbName, String url, String payload, String contentType) {

        Verb verb = Verb.valueOf(verbName.toUpperCase())
        return oauthResourceService.accessResource(findService(serviceName), accessToken, verb, url, payload, contentType, connectTimeout, receiveTimeout)

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

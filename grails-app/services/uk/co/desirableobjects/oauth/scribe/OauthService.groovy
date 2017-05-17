package uk.co.desirableobjects.oauth.scribe

import org.codehaus.groovy.runtime.typehandling.GroovyCastException
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.exceptions.OAuthException
import com.github.scribejava.core.builder.api.BaseApi
import com.github.scribejava.core.httpclient.jdk.JDKHttpClientConfig
import com.github.scribejava.core.model.OAuth1AccessToken
import com.github.scribejava.core.model.OAuth1RequestToken
import com.github.scribejava.core.model.OAuth2AccessToken
import com.github.scribejava.core.model.SignatureType
import com.github.scribejava.core.model.Token
import com.github.scribejava.core.model.Verb
import com.github.scribejava.core.oauth.OAuthService
import org.springframework.beans.factory.InitializingBean
import uk.co.desirableobjects.oauth.scribe.exception.InvalidProviderClassException
import uk.co.desirableobjects.oauth.scribe.exception.UnknownProviderException
import uk.co.desirableobjects.oauth.scribe.util.DynamicMethods
import uk.co.desirableobjects.oauth.scribe.exception.InvalidOauthProviderException
import uk.co.desirableobjects.oauth.scribe.resource.ResourceAccessor

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

                if (!(api in BaseApi)) {
                  throw new InvalidProviderClassException(name, providerConfig.api)
                }

                String callback = providerConfig.containsKey('callback') ? providerConfig.callback : null
                SignatureType signatureType = providerConfig.containsKey('signatureType') ? providerConfig.signatureType : null
                String scope = providerConfig.containsKey('scope') ? providerConfig.scope : null

                final JDKHttpClientConfig clientConfig = JDKHttpClientConfig.defaultConfig()
                clientConfig.setConnectTimeout(connectTimeout)
                clientConfig.setReadTimeout(receiveTimeout)

                ServiceBuilder serviceBuilder = new ServiceBuilder()
                        .apiKey(providerConfig.key as String)
                        .apiSecret(providerConfig.secret as String)
                        .httpClientConfig(clientConfig)

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
                    service: serviceBuilder.build(api.instance()),
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

    private OAuth1RequestToken getRequestToken(String serviceName) {

        return findService(serviceName).getRequestToken()

    }

    String getAuthorizationUrlForOAuth1(String serviceName, OAuth1RequestToken token) {

        return findService(serviceName).getAuthorizationUrl(token)

    }

    String getAuthorizationUrlForOAuth2(String serviceName) {

        return findService(serviceName).getAuthorizationUrl()

    }

    OAuth1AccessToken getAccessTokenForOAuth1(String serviceName, OAuth1RequestToken token, String verifier) {

        return findService(serviceName).getAccessToken(token, verifier)

    }

    OAuth2AccessToken getAccessTokenForOAuth2(String serviceName, String code) {

        return findService(serviceName).getAccessToken(code)

    }

    def methodMissing(String name, args) {

       if( name ==~ /^.*RequestToken/) {

           String provider = DynamicMethods.extractKeyword(name, /^get(.*)RequestToken/)
           return getRequestToken(provider)

       }

       if( name ==~ /^.*AuthorizationUrl/) {

            String provider = DynamicMethods.extractKeyword(name, /^get(.*)AuthorizationUrl/)
            if (args[0]) {
              return getAuthorizationUrlForOAuth1(provider, args[0])
            } else {
              return getAuthorizationUrlForOAuth2(provider)
            }

       }

       if( name ==~ /^.*AccessToken/) {

            String provider = DynamicMethods.extractKeyword(name, /^get(.*)AccessToken/)
            if (args[1]) {
              return getAccessTokenForOAuth1(provider, args[0], args[1])
            } else {
              return getAccessTokenForOAuth2(provider, args[0])
            }

       }

       if( name ==~ /^(get|put|post|delete|options|head).*Resource/) {

              def m = name =~ /^(get|put|post|delete|options|head)(.*)Resource/
              String verb = (String) m[0][1]
              String serviceName = (String) m[0][2].toString().toLowerCase()
              Verb actualVerb = Verb.valueOf(verb.toUpperCase())
              OAuthService service = findService(serviceName)

               ResourceAccessor resourceAccessor = new ResourceAccessor(
                       verb: actualVerb,
                       url: args[1] as String,
                       bodyParameters: (args.length > 2) ? args[2] as Map : null
               )

               if (args.length > 3) {
                    resourceAccessor.headers.putAll(args[3] as Map<String, String>)
               }

               return oauthResourceService.accessResource(service, args[0] as Token, resourceAccessor)

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

            resourceAccessor.headers.putAll(args[3] as Map<String, String>)

            return oauthResourceService.accessResource(service, args[0] as Token, resourceAccessor)

        }

	    def querystringParamsMethodSuffix = "ResourceWithQuerystringParams"
	    if( name ==~ /^(get|put|post|delete|options|head).*${querystringParamsMethodSuffix}/) {

		    def m = name =~ /^(get|put|post|delete|options|head)(.*)${querystringParamsMethodSuffix}/
		    String verb = (String) m[0][1]
		    String serviceName = (String) m[0][2].toString().toLowerCase()

		    Verb actualVerb = Verb.valueOf(verb.toUpperCase())
		    OAuthService service = findService(serviceName)

		    ResourceAccessor resourceAccessor = new ResourceAccessor(
				    verb: actualVerb,
				    url: args[1] as String,
				    querystringParams: (args.length > 2) ? args[2] as Map<String, String> : null
		    )

		    resourceAccessor.headers.putAll(args[3] as Map<String, String>)

		    return oauthResourceService.accessResource(service, args[0] as Token, resourceAccessor)

	    }

	    throw new MissingMethodException(name, getClass(), args)

    }

    protected OAuthService findService(String providerName) {

        return findProviderConfiguration(providerName).service
    }

    OauthProvider findProviderConfiguration(String providerName) {

        if (!services.containsKey(providerName)) {
            throw new UnknownProviderException(providerName)
        }

        return services[providerName]

    }

}

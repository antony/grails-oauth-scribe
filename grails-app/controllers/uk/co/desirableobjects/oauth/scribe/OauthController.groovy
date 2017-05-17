package uk.co.desirableobjects.oauth.scribe

import org.scribe.exceptions.OAuthException
import org.scribe.model.Token
import org.scribe.model.Verifier
import grails.web.servlet.mvc.GrailsParameterMap
import uk.co.desirableobjects.oauth.scribe.exception.MissingRequestTokenException
import uk.co.desirableobjects.oauth.scribe.holder.RedirectHolder

class OauthController {
    private static final Token EMPTY_TOKEN = new Token('', '')

    OauthService oauthService

    def callback() {

        String providerName = params.provider
        OauthProvider provider = oauthService.findProviderConfiguration(providerName)

        Verifier verifier = extractVerifier(provider, params)

        if (!verifier) {
            redirect(uri: provider.failureUri)
            return
        }

        Token requestToken = provider.oauthVersion == SupportedOauthVersion.TWO ?
            new Token(params?.code, "") :
            (Token) session[oauthService.findSessionKeyForRequestToken(providerName)]

        if (!requestToken) {
            throw new MissingRequestTokenException(providerName)
        }

        Token accessToken

        try {
            accessToken = oauthService.getAccessToken(providerName, requestToken, verifier)
        } catch(OAuthException ex){
            log.error("Cannot authenticate with oauth: ${ex.toString()}")
            return redirect(uri: provider.failureUri)
        }

        session[oauthService.findSessionKeyForAccessToken(providerName)] = accessToken
        session.removeAttribute(oauthService.findSessionKeyForRequestToken(providerName))

        return redirect(uri: provider.successUri)

    }

    private Verifier extractVerifier(OauthProvider provider, GrailsParameterMap params) {

        String verifierKey = determineVerifierKey(provider)

        if (!params[verifierKey]) {
            log.error("Cannot authenticate with oauth: Could not find oauth verifier in ${params}.")
            return null
        }

        String verification = params[verifierKey]
        return new Verifier(verification)

    }

    private String determineVerifierKey(OauthProvider provider) {

        return SupportedOauthVersion.TWO == provider.oauthVersion ? 'code' : 'oauth_verifier'

    }

    def authenticate() {

        String providerName = params.provider
        OauthProvider provider = oauthService.findProviderConfiguration(providerName)

        if ( grailsApplication.config.grails.plugin.springsecurity.oauth.setCallbackDynamically ) {
            ServiceBuilder serviceBuilder = new ServiceBuilder()
                    .provider( provider.service.api.class )
                    .apiKey( provider.service.config.apiKey as String )
                    .apiSecret( provider.service.config.apiSecret as String )

            if ( provider.service.config.callback ) {
                String referer = request.getHeader( "referer" )
                String callbackPrefix = referer.replaceAll( new URL( referer ).getPath(), "" )
                serviceBuilder.callback( callbackPrefix + ( grailsApplication.config.oauth.providers[providerName].callback as String ) )
            }

            if ( provider.service.config.signatureType ) {
                serviceBuilder.signatureType( provider.service.config.signatureType as SignatureType )
            }

            if ( provider.service.config.scope ) {
                serviceBuilder.scope( provider.service.config.scope as String )
            }

            provider = new OauthProvider(
                    service: serviceBuilder.build(),
                    successUri: provider.successUri,
                    failureUri: provider.failureUri
            )
        }

        Token requestToken = EMPTY_TOKEN
        if (provider.oauthVersion == SupportedOauthVersion.ONE) {
            requestToken = provider.service.requestToken
        }
        
        if ( grailsApplication.config.grails.plugin.springsecurity.oauth.setCallbackDynamically ) {
            oauthService.services[providerName] = provider
        }

        session[oauthService.findSessionKeyForRequestToken(providerName)] = requestToken
        String url = oauthService.getAuthorizationUrl(providerName, requestToken)

        RedirectHolder.setUri(params.redirectUrl)
        return redirect(url: url)
    }
}

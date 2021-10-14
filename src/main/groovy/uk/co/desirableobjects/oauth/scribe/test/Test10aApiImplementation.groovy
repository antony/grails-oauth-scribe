package uk.co.desirableobjects.oauth.scribe.test

import com.github.scribejava.core.builder.api.DefaultApi10a
import com.github.scribejava.core.model.OAuth1RequestToken

class Test10aApiImplementation extends DefaultApi10a {
    @Override
    String getRequestTokenEndpoint() {
        return null
    }

    @Override
    String getAccessTokenEndpoint() {
        return null
    }

    @Override
    String getAuthorizationUrl(OAuth1RequestToken token) {
        return null
    }
}
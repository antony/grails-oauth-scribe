package uk.co.desirableobjects.oauth.scribe

import com.github.scribejava.core.model.OAuth1AccessToken
import com.github.scribejava.core.model.OAuth1RequestToken
import com.github.scribejava.core.model.OAuthConfig
import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.model.Token
import com.github.scribejava.core.oauth.OAuthService
import java.util.concurrent.ExecutionException

/* This class exists because some methods on OAuth10aService have been made final and can't be mocked by Spock  */
public class MockOAuth1Service extends OAuthService<OAuth1AccessToken> {

    public MockOAuth1Service() {
        super(new OAuthConfig('', ''))
    }

    public MockOAuth1Service(OAuthConfig config) {
        super(config)
    }

    public String getVersion() {
        return '1.0'
    }

    public void signRequest(OAuth1AccessToken token, OAuthRequest request) { }

    public OAuth1RequestToken getRequestToken() throws IOException, InterruptedException, ExecutionException {
       return new OAuth1RequestToken('a', 'b', 'c')
    }

    public OAuth1AccessToken getAccessToken(OAuth1RequestToken requestToken, String oauthVerifier)
            throws IOException, InterruptedException, ExecutionException {
        return new OAuth1AccessToken('d', 'e', 'f')
    }

}

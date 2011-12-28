package uk.co.desirableobjects.oauth.scribe.test

import org.scribe.builder.api.DefaultApi10a

class Test10aApiImplementation extends DefaultApi10a {

    @Override
    String getRequestTokenEndpoint() {
        return null  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    String getAccessTokenEndpoint() {
        return null  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    String getAuthorizationUrl(org.scribe.model.Token token) {
        return null  //To change body of implemented methods use File | Settings | File Templates.
    }

}

package uk.co.desirableobjects.oauth.scribe.holder

import org.springframework.web.context.request.RequestContextHolder

class RedirectHolder {
    private static def HASH_NAME = "oauthPluginRedirectHash"
    private static def URI_NAME = "uri"
    private static def DEFAULT_URI = "/"

    public static void setUri(uri) {
        if (!uri || uri.empty) {
            return
        }

        getOrCreateRedirectHash().putAt(URI_NAME, uri)
    }

    public static def getRedirect() {
        return getStorage().getAt(HASH_NAME) ?: getDefaultRedirect()
    }

    public static void setRedirectHash(redirectHash) {
        if (redirectHash) {
            getOrCreateRedirectHash().putAll(redirectHash)
        }
    }

    protected static def getStorage() {
        return RequestContextHolder.currentRequestAttributes().getSession()
    }

    protected static def getOrCreateRedirectHash() {
        def hash = getStorage()
        if (!hash.getAt(HASH_NAME)) {
            hash.putAt(HASH_NAME, [:])
        }
        return hash.getAt(HASH_NAME)
    }

    protected static Map<String, String> getDefaultRedirect() {
        Map<String, String> hash = [:]
        hash.put(URI_NAME, DEFAULT_URI)
        return hash
    }
}

package uk.co.desirableobjects.oauth.scribe.holder

import org.springframework.web.context.request.RequestContextHolder

/**
 * User: AlekseyLeshko
 * Date: 22/07/13
 * Time: 19:27
 * To change this template use File | Settings | File Templates.
 */

class RedirectHolder {
    private static def HASH_NAME = "oauthPluginRedirectHash"
    private static def URI_NAME = "uri"
    private static def DEFAULT_URI = "/"

    public static def setUri(uri) {
        if (!uri || uri.empty) {
            return
        }

        getOrCreateRedirectHash().putAt(URI_NAME, uri)
    }

    public static def getRedirect() {
        getStorage().getAt(HASH_NAME) ?: getDefaultRedirect()
    }

    public static void setRedirectHash(redirectHash) {
        if (!redirectHash) {
            return
        }

        getOrCreateRedirectHash().putAll(redirectHash)
    }

    protected static def getStorage() {
        RequestContextHolder.currentRequestAttributes().getSession()
    }

    protected static def getOrCreateRedirectHash() {
        def hash = getStorage()
        if (!hash.getAt(HASH_NAME)) {
            hash.putAt(HASH_NAME, [:])
        }
        hash.getAt(HASH_NAME)
    }

    protected static def getDefaultRedirect() {
        def hash = [:]
        hash.put(URI_NAME, DEFAULT_URI)
        hash
    }
}

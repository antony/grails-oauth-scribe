
package grails.plugins.uk.co.desirableobjects.oauth.scribe

import org.gmock.WithGMock

@WithGMock
class GMockAddon {

    boolean simulate(Closure closure) {

        Throwable th = null

        play {
            try {
                closure.call()
            } catch (Throwable t) {
                th = t
            }
        }

        if (th) {
            throw th
        }

        return true
    }
}
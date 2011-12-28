class OauthScribeGrailsPlugin {

    def version = "1.2"

    def grailsVersion = "1.3.1 > *"

    def dependsOn = [:]

    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "Antony Jones"
    def authorEmail = "aj#desirableobjects.co.uk"
    def title = "Oauth Scribe Plugin"
    def description = 'Provides oAuth integration for Grails, using the Scribe framework'

    def documentation = "http://grails.org/plugin/grails-oauth-scribe"

}

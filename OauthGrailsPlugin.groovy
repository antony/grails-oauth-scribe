class OauthGrailsPlugin {

    def version = "2.0"

    def grailsVersion = "1.3.1 > *"

    def dependsOn = [:]

    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "Antony Jones"
    def authorEmail = "aj#desirableobjects.co.uk"
    def title = "Oauth Plugin"
    def description = 'Provides oAuth integration for Grails, using the Scribe framework'

    def documentation = "http://aiten.github.com/grails-oauth-scribe/"
    def scm = [url: "https://github.com/aiten/grails-oauth-scribe"]
}

package uk.co.desirableobjects.oauth.scribe


import grails.plugins.Plugin

class OauthGrailsPlugin extends Plugin {
    def grailsVersion = "4.0.0 > *"
    def author = "Antony Jones, Peter Ledbrook"
    def authorEmail = "aj@desirableobjects.co.uk"
    def title = "Oauth Plugin"
    def description = 'Provides oAuth integration for Grails, using the Scribe framework'
    def documentation = "http://antony.github.com/grails-oauth-scribe/"
    def scm = [url: "https://github.com/antony/grails-oauth-scribe"]
    def profiles = ['web']
    def developers = [
        [ name: "Dhiraj Mahapatro", email: "dmahapatro@netjets.com" ]
    ]
    def issueManagement = [ system: "Github", url: "https://github.com/antony/grails-oauth-scribe" ]
}

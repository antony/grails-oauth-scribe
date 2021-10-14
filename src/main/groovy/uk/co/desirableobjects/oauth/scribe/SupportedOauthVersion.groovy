package uk.co.desirableobjects.oauth.scribe

enum SupportedOauthVersion {

    ONE('1.0'), TWO('2.0')

    String scribeVersion

    private SupportedOauthVersion(String scribeVersion) {
        this.scribeVersion = scribeVersion
    }

    static SupportedOauthVersion parse(String versionString) {
        return values().find { SupportedOauthVersion supportedVersion ->
            supportedVersion.scribeVersion == versionString
        }
    }
}
grails.project.work.dir = "target"

grails.project.dependency.resolver = "maven"
grails.project.dependency.resolution = {

    inherits "global"
    log "warn"

    repositories {
        grailsCentral()

        mavenLocal
        mavenCentral()
        mavenRepo 'http://repo.desirableobjects.co.uk'
        mavenRepo 'https://raw.github.com/fernandezpablo85/scribe-java/mvn-repo'
    }

    dependencies {

        runtime 'org.scribe:scribe:1.3.7'

        test    'org.gmock:gmock:0.8.2',
                'org.objenesis:objenesis:1.2'

    }

    plugins {

        build(':release:3.0.1', ':rest-client-builder:2.0.1') {
            export = false
        }
    }
}

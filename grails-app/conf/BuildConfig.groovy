grails.project.work.dir = "target"

grails.project.dependency.resolution = {

    inherits "global"
    log "warn"

    repositories {
        grailsCentral()

        mavenLocal
        mavenCentral()
        mavenRepo 'http://repo.desirableobjects.co.uk'
    }

    dependencies {

        runtime 'org.scribe:scribe:1.3.5'
        compile 'org.spockframework:spock-core:0.7-groovy-2.0'

        test    'org.gmock:gmock:0.8.2',
                'org.objenesis:objenesis:1.2'

    }

    plugins {

        compile ':spock:0.7', {
            export = false
        }

        build(':release:2.2.0', ':rest-client-builder:1.0.3') {
            export = false
        }
    }
}

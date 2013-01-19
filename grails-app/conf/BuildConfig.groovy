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

        runtime 'org.scribe:scribe:1.3.2'

        test    'org.gmock:gmock:0.8.2',
                'org.objenesis:objenesis:1.2'

    }

    plugins {

        test ':spock:0.6'

        build ':release:2.0.4', {
            export = false
        }

    }
}

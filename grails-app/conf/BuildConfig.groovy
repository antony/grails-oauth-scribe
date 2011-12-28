grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.release.scm.enabled = false

grails.project.dependency.resolution = {

    inherits("global") {
    }

    log "warn"
    
    repositories {
        grailsPlugins()
        grailsHome()
        grailsCentral()

        mavenLocal(null)
        mavenCentral()
        mavenRepo 'http://repo.desirableobjects.co.uk'
    }
    dependencies {

        runtime 'org.scribe:scribe:1.3.0'
        test    'org.gmock:gmock:0.8.2'

    }
    plugins {

        test ':spock:0.6-SNAPSHOT'

        build ':release:1.0.0', {
            export = false
        }

    }
}

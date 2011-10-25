grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.dependency.resolution = {

    inherits("global") {
    }

    log "warn"
    
    repositories {
        grailsPlugins()
        grailsHome()
        grailsCentral()

        mavenLocal()
        mavenCentral()
    }
    dependencies {

        runtime 'org.scribe:scribe:1.2.3'

        test    'org.gmock:gmock:0.8.1'
    }
    plugins {

        test(':spock:0.5-groovy-1.7') {
            export = false
        }

    }
}

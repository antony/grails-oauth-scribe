package uk.co.desirableobjects.oauth.scribe.util

class DynamicMethods {

    static String extractKeyword(String source, String expression) {

        def m = source =~ expression
        return (String) m[0][1].toString().toLowerCase()

    }

}

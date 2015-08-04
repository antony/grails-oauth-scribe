package uk.co.desirableobjects.oauth.scribe.resource

import groovy.transform.EqualsAndHashCode
import org.scribe.model.Verb

@EqualsAndHashCode
class ResourceAccessor {

    int connectTimeout
    int receiveTimeout

    Verb verb
    String url
    byte[] payload
    Map<String, String> headers = [:]
    Map<String, String> bodyParameters = [:]
	Map<String, String> querystringParams = [:]

    void setPayload(byte[] data) {
        headers.put('Content-Length', data.length as String)
        payload = data
    }

    void setHeaders(Map headers) {
        throw new IllegalAccessException("Setting headers would overwrite auto-generated header data. Use addHeader to add a new or override an existing header")
    }

    void addHeader(String name, String value) {
        headers.put(name, value)
    }

}

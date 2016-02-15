package org.ovirt.engine.core.common.businessentities;

import java.util.Map;

public class HttpLocationInfo extends LocationInfo {
    String url;
    Map<String, String> headers;

    public HttpLocationInfo(String url, Map<String, String> headers) {
        super(ConnectionMethod.HTTP);
        this.url = url;
        this.headers = headers;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    @Override
    public String toString() {
        return "HttpLocationInfo [url=" + url + ", headers=" + headers + "]";
    }
}

package org.si.as3.net;


public class URLRequest {

    public String url;
    public String method = "GET";
    public Object data;

    public URLRequest(String url) {
        this.url = url;
    }

    public URLRequest() {
    }
}

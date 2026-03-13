package com.example.integration.http.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

public class ThirdPartyRequest {

    private HttpMethod method;
    private String endpoint;
    private MediaType contentType = MediaType.APPLICATION_JSON;
    private Map<String, Object> params = new LinkedHashMap<>();
    private Map<String, String> headers = new LinkedHashMap<>();

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public MediaType getContentType() {
        return contentType;
    }

    public void setContentType(MediaType contentType) {
        this.contentType = contentType;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params == null ? Collections.emptyMap() : params;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers == null ? Collections.emptyMap() : headers;
    }
}

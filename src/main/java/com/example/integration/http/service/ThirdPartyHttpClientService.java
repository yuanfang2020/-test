package com.example.integration.http.service;

import com.example.integration.http.config.ThirdPartyClientProperties;
import com.example.integration.http.crypto.FieldEncryptionService;
import com.example.integration.http.crypto.PayloadEncryptionService;
import com.example.integration.http.exception.ThirdPartyRequestException;
import com.example.integration.http.model.ThirdPartyRequest;
import com.example.integration.http.signature.SignatureService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;

public class ThirdPartyHttpClientService {

    private static final Logger log = LoggerFactory.getLogger(ThirdPartyHttpClientService.class);

    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ThirdPartyClientProperties properties;
    private final FieldEncryptionService fieldEncryptionService;
    private final PayloadEncryptionService payloadEncryptionService;
    private final SignatureService signatureService;

    public ThirdPartyHttpClientService(CloseableHttpClient httpClient,
                                       ObjectMapper objectMapper,
                                       ThirdPartyClientProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.fieldEncryptionService = new FieldEncryptionService(properties);
        this.payloadEncryptionService = new PayloadEncryptionService(properties);
        this.signatureService = new SignatureService(properties);
    }

    public String execute(ThirdPartyRequest request) {
        long start = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString().replace("-", "");
        try {
            validate(request);
            Map<String, Object> encryptedParams = fieldEncryptionService.encryptSensitiveFields(request.getParams());
            HttpRequestBase httpRequest = buildHttpRequest(request, encryptedParams);
            bindHeaders(httpRequest, request, encryptedParams);
            log.info("[third-party][request] traceId={}, method={}, url={}, headers={}, body={}",
                    traceId, request.getMethod(), httpRequest.getURI(), httpRequest.getAllHeaders(), preview(encryptedParams));

            try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
                String responseBody = response.getEntity() == null
                        ? ""
                        : EntityUtils.toString(response.getEntity(), Charset.forName(properties.getCharset()));
                log.info("[third-party][response] traceId={}, status={}, costMs={}, body={}",
                        traceId, response.getStatusLine().getStatusCode(), System.currentTimeMillis() - start, responseBody);
                return responseBody;
            }
        } catch (Exception ex) {
            log.error("[third-party][error] traceId={}, costMs={}, message={}",
                    traceId, System.currentTimeMillis() - start, ex.getMessage(), ex);
            throw ex instanceof ThirdPartyRequestException
                    ? (ThirdPartyRequestException) ex
                    : new ThirdPartyRequestException("Third-party invoke failed", ex);
        }
    }

    private HttpRequestBase buildHttpRequest(ThirdPartyRequest request, Map<String, Object> params) throws Exception {
        if (HttpMethod.GET.equals(request.getMethod())) {
            URIBuilder builder = new URIBuilder(properties.getBaseUrl() + request.getEndpoint());
            if (properties.isEncryptGetQuery()) {
                builder.addParameter("cipherText", payloadEncryptionService.encryptPayload(toJson(params)));
            } else {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    builder.addParameter(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            return new HttpGet(builder.build());
        }

        if (HttpMethod.POST.equals(request.getMethod())) {
            HttpPost httpPost = new HttpPost(properties.getBaseUrl() + request.getEndpoint());
            String requestBody = buildPostBody(request.getContentType(), params);
            String encrypted = payloadEncryptionService.encryptPayload(requestBody);
            httpPost.setEntity(new StringEntity(encrypted, Consts.UTF_8));
            httpPost.setHeader("Content-Type", request.getContentType().toString());
            return httpPost;
        }
        throw new ThirdPartyRequestException("Unsupported method: " + request.getMethod());
    }

    private String buildPostBody(MediaType contentType, Map<String, Object> params) {
        if (MediaType.APPLICATION_FORM_URLENCODED.equals(contentType)) {
            List<NameValuePair> pairs = new ArrayList<>();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                pairs.add(new BasicNameValuePair(entry.getKey(), String.valueOf(entry.getValue())));
            }
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < pairs.size(); i++) {
                NameValuePair pair = pairs.get(i);
                if (i > 0) {
                    builder.append("&");
                }
                builder.append(pair.getName()).append("=").append(pair.getValue());
            }
            return builder.toString();
        }
        return toJson(params);
    }

    private void bindHeaders(HttpRequestBase httpRequest, ThirdPartyRequest request, Map<String, Object> params) {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String payload = HttpMethod.GET.equals(request.getMethod()) ? preview(params) : toJson(params);
        String signature = signatureService.sign(request.getEndpoint(), timestamp, nonce, payload);

        httpRequest.setHeader(properties.getAuthHeader(), properties.getAuthToken());
        httpRequest.setHeader("X-App-Id", properties.getAppId());
        httpRequest.setHeader(properties.getTimestampHeader(), timestamp);
        httpRequest.setHeader(properties.getNonceHeader(), nonce);
        httpRequest.setHeader(properties.getSignatureHeader(), signature);

        if (!CollectionUtils.isEmpty(request.getHeaders())) {
            request.getHeaders().forEach(httpRequest::setHeader);
        }
    }

    private void validate(ThirdPartyRequest request) {
        if (request == null || request.getMethod() == null || request.getEndpoint() == null) {
            throw new ThirdPartyRequestException("Request/method/endpoint must not be null");
        }
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            throw new ThirdPartyRequestException("Object to json failed", ex);
        }
    }

    private String preview(Object object) {
        String json = object instanceof String ? (String) object : toJson(object);
        return json.length() > 1000 ? json.substring(0, 1000) + "..." : json;
    }
}

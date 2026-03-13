package com.example.integration.http.config;

import com.example.integration.http.crypto.EncryptionAlgorithm;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "third-party.http")
public class ThirdPartyClientProperties {

    private String baseUrl;
    private String appId;
    private String authToken;
    private String signatureSecret;
    private String signatureHeader = "X-Signature";
    private String timestampHeader = "X-Timestamp";
    private String nonceHeader = "X-Nonce";
    private String authHeader = "Authorization";
    private EncryptionAlgorithm fieldEncryptionAlgorithm = EncryptionAlgorithm.AES;
    private EncryptionAlgorithm payloadEncryptionAlgorithm = EncryptionAlgorithm.AES;
    private String fieldKey;
    private String payloadKey;
    private boolean enableFieldEncryption = true;
    private boolean enablePayloadEncryption = true;
    private boolean encryptGetQuery = false;
    private Set<String> sensitiveFields = new LinkedHashSet<>(Arrays.asList(
            "mobile", "phone", "cardNo", "bankCard", "idCard", "certNo"));
    private String charset = StandardCharsets.UTF_8.name();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getSignatureSecret() {
        return signatureSecret;
    }

    public void setSignatureSecret(String signatureSecret) {
        this.signatureSecret = signatureSecret;
    }

    public String getSignatureHeader() {
        return signatureHeader;
    }

    public void setSignatureHeader(String signatureHeader) {
        this.signatureHeader = signatureHeader;
    }

    public String getTimestampHeader() {
        return timestampHeader;
    }

    public void setTimestampHeader(String timestampHeader) {
        this.timestampHeader = timestampHeader;
    }

    public String getNonceHeader() {
        return nonceHeader;
    }

    public void setNonceHeader(String nonceHeader) {
        this.nonceHeader = nonceHeader;
    }

    public String getAuthHeader() {
        return authHeader;
    }

    public void setAuthHeader(String authHeader) {
        this.authHeader = authHeader;
    }

    public EncryptionAlgorithm getFieldEncryptionAlgorithm() {
        return fieldEncryptionAlgorithm;
    }

    public void setFieldEncryptionAlgorithm(EncryptionAlgorithm fieldEncryptionAlgorithm) {
        this.fieldEncryptionAlgorithm = fieldEncryptionAlgorithm;
    }

    public EncryptionAlgorithm getPayloadEncryptionAlgorithm() {
        return payloadEncryptionAlgorithm;
    }

    public void setPayloadEncryptionAlgorithm(EncryptionAlgorithm payloadEncryptionAlgorithm) {
        this.payloadEncryptionAlgorithm = payloadEncryptionAlgorithm;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public void setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
    }

    public String getPayloadKey() {
        return payloadKey;
    }

    public void setPayloadKey(String payloadKey) {
        this.payloadKey = payloadKey;
    }

    public boolean isEnableFieldEncryption() {
        return enableFieldEncryption;
    }

    public void setEnableFieldEncryption(boolean enableFieldEncryption) {
        this.enableFieldEncryption = enableFieldEncryption;
    }

    public boolean isEnablePayloadEncryption() {
        return enablePayloadEncryption;
    }

    public void setEnablePayloadEncryption(boolean enablePayloadEncryption) {
        this.enablePayloadEncryption = enablePayloadEncryption;
    }

    public boolean isEncryptGetQuery() {
        return encryptGetQuery;
    }

    public void setEncryptGetQuery(boolean encryptGetQuery) {
        this.encryptGetQuery = encryptGetQuery;
    }

    public Set<String> getSensitiveFields() {
        return sensitiveFields;
    }

    public void setSensitiveFields(Set<String> sensitiveFields) {
        this.sensitiveFields = sensitiveFields;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }
}

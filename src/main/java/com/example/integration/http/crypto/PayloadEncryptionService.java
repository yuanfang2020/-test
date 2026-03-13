package com.example.integration.http.crypto;

import com.example.integration.http.config.ThirdPartyClientProperties;

public class PayloadEncryptionService {

    private final ThirdPartyClientProperties properties;

    public PayloadEncryptionService(ThirdPartyClientProperties properties) {
        this.properties = properties;
    }

    public String encryptPayload(String plainText) {
        if (!properties.isEnablePayloadEncryption()) {
            return plainText;
        }
        return CryptoSupport.encrypt(plainText, properties.getPayloadKey(), properties.getPayloadEncryptionAlgorithm());
    }
}

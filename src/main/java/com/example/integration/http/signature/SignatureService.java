package com.example.integration.http.signature;

import com.example.integration.http.config.ThirdPartyClientProperties;
import com.example.integration.http.exception.ThirdPartyRequestException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SignatureService {

    private final ThirdPartyClientProperties properties;

    public SignatureService(ThirdPartyClientProperties properties) {
        this.properties = properties;
    }

    public String sign(String endpoint, String timestamp, String nonce, String payload) {
        String signText = endpoint + "|" + timestamp + "|" + nonce + "|" + payload;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getSignatureSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(signText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception ex) {
            throw new ThirdPartyRequestException("Signature failed", ex);
        }
    }
}

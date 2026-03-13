package com.example.integration.http.crypto;

import com.example.integration.http.exception.ThirdPartyRequestException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public final class CryptoSupport {

    private CryptoSupport() {
    }

    public static String encrypt(String plainText, String key, EncryptionAlgorithm algorithm) {
        if (algorithm == EncryptionAlgorithm.NONE) {
            return plainText;
        }
        try {
            Cipher cipher = Cipher.getInstance(resolveTransformation(algorithm));
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(buildKey(key, algorithm), algorithm.name()));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (GeneralSecurityException ex) {
            throw new ThirdPartyRequestException("Encrypt failed, algorithm=" + algorithm, ex);
        }
    }

    private static String resolveTransformation(EncryptionAlgorithm algorithm) {
        switch (algorithm) {
            case AES:
                return "AES/ECB/PKCS5Padding";
            case DES:
                return "DES/ECB/PKCS5Padding";
            default:
                throw new ThirdPartyRequestException("Unsupported algorithm=" + algorithm);
        }
    }

    private static byte[] buildKey(String key, EncryptionAlgorithm algorithm) {
        int keySize = algorithm == EncryptionAlgorithm.DES ? 8 : 16;
        byte[] source = key.getBytes(StandardCharsets.UTF_8);
        byte[] target = new byte[keySize];
        for (int i = 0; i < keySize; i++) {
            target[i] = i < source.length ? source[i] : 0;
        }
        return target;
    }
}

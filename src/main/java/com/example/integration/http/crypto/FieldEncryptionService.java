package com.example.integration.http.crypto;

import com.example.integration.http.config.ThirdPartyClientProperties;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FieldEncryptionService {

    private final ThirdPartyClientProperties properties;

    public FieldEncryptionService(ThirdPartyClientProperties properties) {
        this.properties = properties;
    }

    public Map<String, Object> encryptSensitiveFields(Map<String, Object> source) {
        if (!properties.isEnableFieldEncryption() || source == null || source.isEmpty()) {
            return source;
        }
        return encryptMap(source, properties.getSensitiveFields());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> encryptMap(Map<String, Object> source, Set<String> sensitiveFields) {
        Map<String, Object> target = new LinkedHashMap<>(source.size());
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                value = encryptMap((Map<String, Object>) value, sensitiveFields);
            } else if (value instanceof List) {
                value = encryptList((List<Object>) value, sensitiveFields);
            } else if (isSensitive(key, sensitiveFields) && value != null) {
                value = CryptoSupport.encrypt(String.valueOf(value),
                        properties.getFieldKey(), properties.getFieldEncryptionAlgorithm());
            }
            target.put(key, value);
        }
        return target;
    }

    @SuppressWarnings("unchecked")
    private List<Object> encryptList(List<Object> source, Set<String> sensitiveFields) {
        List<Object> target = new ArrayList<>(source.size());
        for (Object item : source) {
            if (item instanceof Map) {
                target.add(encryptMap((Map<String, Object>) item, sensitiveFields));
            } else if (item instanceof List) {
                target.add(encryptList((List<Object>) item, sensitiveFields));
            } else {
                target.add(item);
            }
        }
        return target;
    }

    private boolean isSensitive(String key, Set<String> sensitiveFields) {
        return sensitiveFields.contains(key) || sensitiveFields.contains(key.toLowerCase(Locale.ROOT));
    }
}

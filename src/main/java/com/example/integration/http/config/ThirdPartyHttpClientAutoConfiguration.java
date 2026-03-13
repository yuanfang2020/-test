package com.example.integration.http.config;

import com.example.integration.http.service.ThirdPartyHttpClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ThirdPartyClientProperties.class)
public class ThirdPartyHttpClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CloseableHttpClient closeableHttpClient() {
        return HttpClients.createDefault();
    }

    @Bean
    @ConditionalOnMissingBean
    public ThirdPartyHttpClientService thirdPartyHttpClientService(CloseableHttpClient httpClient,
                                                                   ObjectMapper objectMapper,
                                                                   ThirdPartyClientProperties properties) {
        return new ThirdPartyHttpClientService(httpClient, objectMapper, properties);
    }
}

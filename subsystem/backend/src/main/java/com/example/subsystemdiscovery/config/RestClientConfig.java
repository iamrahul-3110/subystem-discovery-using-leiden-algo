package com.example.subsystemdiscovery.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@Slf4j
public class RestClientConfig {

        @Bean
        RestClient llmRestClient(LlmProperties properties) {
            log.info(
                    "Initializing LLM RestClient. ConnectTimeout={}ms ReadTimeout={}ms",
                    properties.getConnectTimeoutMs(),
                    properties.getReadTimeoutMs());
        
            SimpleClientHttpRequestFactory factory =
                    new SimpleClientHttpRequestFactory();
        
            factory.setConnectTimeout(
                    properties.getConnectTimeoutMs());
        
            factory.setReadTimeout(
                    properties.getReadTimeoutMs());
        
            return RestClient.builder()
                    .requestFactory(factory)
                    .build();
        }
}

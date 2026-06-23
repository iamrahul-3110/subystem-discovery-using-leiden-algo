package com.example.subsystemdiscovery.llm;

import com.example.subsystemdiscovery.config.LlmProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LlmClientService {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final RestClient restClient;
    private final LlmProperties properties;

    public LlmClientService(@Qualifier("llmRestClient") RestClient restClient,
                            LlmProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public JsonNode callLlm(String userContent, String llmModel) {
        String model = StringUtils.hasText(llmModel) ? llmModel : properties.getModelId();
        Map<String, Object> body = buildChatRequest(userContent);
        return restClient.post()
                .uri(CHAT_COMPLETIONS_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .header("X-Service-Id",    properties.getServiceId())
                .header("user_identifier", properties.getUserIdentifier())
                .header("model",           model)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    private Map<String, Object> buildChatRequest(String userContent) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role",    "user");
        message.put("content", userContent);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messages",    List.of(message));
        body.put("max_tokens",  properties.getMaxTokens());
        body.put("temperature", properties.getTemperature());
        body.put("top_k",       properties.getTopK());
        return body;
    }

    public String extractContent(JsonNode response) {
        if (response == null) {
            return null;
        }
        JsonNode choices = response.get("choices");
        if (choices != null && choices.isArray() && !choices.isEmpty()) {
            JsonNode content = choices.get(0).path("message").path("content");
            if (content.isTextual()) {
                return content.asText();
            }
        }
        JsonNode outputText = response.get("output_text");
        if (outputText != null && outputText.isTextual()) {
            return outputText.asText();
        }
        return null;
    }
}

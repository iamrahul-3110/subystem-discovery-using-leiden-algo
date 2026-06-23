package com.example.subsystemdiscovery.poc.llm;

import com.example.subsystemdiscovery.config.LlmProperties;
import com.example.subsystemdiscovery.subsystem.dto.SubsystemDiscoveryResponse;
import com.example.subsystemdiscovery.subsystem.dto.SubsystemDto;
import com.example.subsystemdiscovery.subsystem.dto.SubsystemLinkDto;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RealLlmProvider implements LlmProvider {

        private static final Logger log = LoggerFactory.getLogger(RealLlmProvider.class);
        private final RestClient restClient;
        private final LlmProperties properties;

        public RealLlmProvider(@Qualifier("llmRestClient") RestClient restClient,
                        LlmProperties properties) {
                this.restClient = restClient;
                this.properties = properties;
        }

        @Override
        public String getProviderName() {
                return "REAL";
        }

    @Override
    public String generateSummary(
            SubsystemDiscoveryResponse response,
            String summaryType,
            String llmModel) {

        if (!properties.isEnabled()) {
            log.warn(
                    "Real LLM summary requested, but LLM integration is disabled.");

            throw new IllegalStateException(
                    "LLM integration is disabled.");
        }

        log.info(
                "Generating Real LLM summary for application '{}' (type='{}', model='{}')",
                response.applicationKey(),
                summaryType,
                llmModel);

        String prompt =
                buildPrompt(
                        response,
                        summaryType);

        String model =
                StringUtils.hasText(llmModel)
                        ? llmModel
                        : "deepseek/deepseek-chat";

        log.info(
                "Prompt length={} chars. Using model={}",
                prompt.length(),
                model);

        if (model.startsWith("gemini")) {
            return generateGeminiSummary(prompt);
        }

        return generateOpenRouterSummary(
                prompt,
                model);
    }

    private String generateOpenRouterSummary(
            String prompt,
            String model) {

        Map<String, Object> body =
                new LinkedHashMap<>();

        body.put(
                "model",
                model);

        body.put(
                "messages",
                List.of(
                        Map.of(
                                "role",
                                "user",
                                "content",
                                prompt)));

        body.put(
                "temperature",
                properties.getTemperature());

        body.put(
                "max_tokens",
                properties.getMaxTokens());

        log.info(
                "Calling OpenRouter model={}",
                model);

        JsonNode response =
                restClient.post()
                        .uri(
                                properties.getOpenrouterUrl())
                        .header(
                                "Authorization",
                                "Bearer "
                                        + properties.getOpenrouterApiKey())
                        .header(
                                "HTTP-Referer",
                                "http://localhost:8081")
                        .header(
                                "X-Title",
                                "Subsystem Discovery POC")
                        .contentType(
                                MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException(
                    "OpenRouter returned null response");
        }

        JsonNode choices =
                response.path("choices");

        if (!choices.isArray()
                || choices.isEmpty()) {

            throw new IllegalStateException(
                    "OpenRouter response contains no choices: "
                            + response);
        }

        String content =
                choices.get(0)
                        .path("message")
                        .path("content")
                        .asText();

        if (!StringUtils.hasText(content)) {

            throw new IllegalStateException(
                    "OpenRouter returned empty content: "
                            + response);
        }

        return content.trim();
    }

        private String generateGeminiSummary(
                String prompt) {

            String url =
                    "https://generativelanguage.googleapis.com/v1beta/models/"
                            + "gemini-2.5-flash:generateContent?key="
                            + properties.getGeminiApiKey();

            Map<String,Object> body =
                    Map.of(
                            "contents",
                            List.of(
                                    Map.of(
                                            "parts",
                                            List.of(
                                                    Map.of(
                                                            "text",
                                                            prompt)))));

            JsonNode response =
                    restClient.post()
                            .uri(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(body)
                            .retrieve()
                            .body(JsonNode.class);

            return response
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        }

        private Map<String, Object> buildOllamaRequest(
                        String prompt,
                        String model) {

                Map<String, Object> message = new LinkedHashMap<>();

                message.put("role", "user");
                message.put("content", prompt);

                Map<String, Object> body = new LinkedHashMap<>();

                body.put("model", model);
                body.put("messages", List.of(message));
                body.put("stream", false);

                // Configure context window size and options using properties
                Map<String, Object> options = new LinkedHashMap<>();
                options.put("num_ctx", properties.getMaxTokens());
                options.put("temperature", properties.getTemperature());
                if (properties.getTopK() > 0) {
                        options.put("top_k", properties.getTopK());
                }
                body.put("options", options);

                return body;
        }


        private String buildPrompt(SubsystemDiscoveryResponse response, String summaryType) {
                String resolvedType = StringUtils.hasText(summaryType) ? summaryType : "MEDIUM_DETAILED";

                if ("LESS_DETAILED".equalsIgnoreCase(resolvedType)) {
                        return buildExecutiveOverviewPrompt(response);
                } else if ("COMPLETE_DETAILED".equalsIgnoreCase(resolvedType)) {
                        return buildDetailedAnalysisPrompt(response);
                } else {
                        return buildArchitectureSummaryPrompt(response);
                }
        }

        private String buildExecutiveOverviewPrompt(SubsystemDiscoveryResponse response) {

                StringBuilder prompt = new StringBuilder();

                prompt.append("""
                                You are a senior engineering leader preparing an executive application assessment.

                                DO NOT discuss:
                                - Leiden algorithm
                                - Graph clustering
                                - Nodes
                                - Edges
                                - Stability scores
                                - Internal graph metrics

                                Focus on:

                                1. What this application appears to do.
                                2. The business capabilities it provides.
                                3. Major functional areas.
                                4. How the major modules collaborate.
                                5. Current strengths.
                                6. Improvement opportunities.
                                7. Recommended next steps.

                                Audience:
                                - Product managers
                                - Engineering managers
                                - Business stakeholders

                                Length:
                                Approximately 300 words.

                                Output sections:

                                Executive Overview
                                Business Capabilities
                                Major Functional Areas
                                Current Strengths
                                Improvement Opportunities
                                Recommendations

                                ================ APPLICATION CONTEXT ================

                                """);

                prompt.append("Application Name: ")
                                .append(response.applicationKey())
                                .append("\n\n");

                prompt.append("Discovered Functional Areas:\n");

                response.subsystems().stream()
                                .limit(8)
                                .forEach(subsystem -> appendBusinessSubsystem(prompt, subsystem));

                prompt.append("\nModule Interactions:\n");

                response.subsystemLinks()
                                .stream()
                                .limit(5)
                                .forEach(link -> appendLink(prompt, link));

                return prompt.toString();
        }

        private String buildArchitectureSummaryPrompt(
                        SubsystemDiscoveryResponse response) {

                StringBuilder prompt = new StringBuilder();

                prompt.append("""
                                You are a Principal Software Architect reviewing an enterprise application.

                                Do NOT focus on:
                                - Graph metrics
                                - Node counts
                                - Edge counts
                                - Leiden algorithm details

                                Focus on the application architecture itself.

                                Provide:

                                1. Application Purpose
                                2. Functional Domains
                                3. Responsibilities of each subsystem
                                4. End-to-end request flow
                                5. Data flow between modules
                                6. Integration points
                                7. Maintainability assessment
                                8. Scalability assessment
                                9. Security observations
                                10. Technology modernization opportunities
                                11. Future architecture evolution

                                Identify opportunities for:

                                - Microservices
                                - Event Driven Architecture
                                - CQRS
                                - Redis Caching
                                - API Modernization
                                - AI Integration

                                Audience:
                                - Architects
                                - Technical Leads
                                - Senior Developers

                                Length:
                                Approximately 600 words.

                                Output Sections:

                                Application Overview
                                Business Domains
                                Architecture Flow
                                Integration Analysis
                                Scalability Review
                                Technology Opportunities
                                Improvement Recommendations

                                ================ APPLICATION CONTEXT ================

                                """);

                prompt.append("Application Name: ")
                                .append(response.applicationKey())
                                .append("\n\n");

                response.subsystems()
                                .stream()
                                .limit(12)
                                .forEach(subsystem -> appendBusinessSubsystem(prompt, subsystem));

                prompt.append("\nSubsystem Interactions:\n");

                response.subsystemLinks()
                                .stream()
                                .limit(10)
                                .forEach(link -> appendLink(prompt, link));

                return prompt.toString();
        }

        private String buildDetailedAnalysisPrompt(SubsystemDiscoveryResponse response) {

                StringBuilder prompt = new StringBuilder();

                prompt.append("""
                                You are a Staff+ Software Architect performing a deep application assessment.

                                IMPORTANT:

                                Treat discovered subsystems as business and technical modules.

                                Do NOT explain:

                                - Leiden algorithm
                                - Clustering mechanics
                                - Stability calculations
                                - Graph implementation details

                                Create a detailed onboarding document for a new engineer joining the project.

                                Analyze:

                                1. Business problem solved by the application.
                                2. Major domains and responsibilities.
                                3. End-to-end request lifecycle.
                                4. Layer breakdown:
                                   - API Layer
                                   - Service Layer
                                   - Domain Layer
                                   - Repository Layer
                                   - Integration Layer

                                5. Module ownership and responsibilities.
                                6. Cross-module dependencies.
                                7. Potential bottlenecks.
                                8. Technical debt areas.
                                9. Scalability concerns.
                                10. Security considerations.
                                11. Extensibility opportunities.
                                12. AI integration opportunities.
                                13. Recommended technologies.
                                14. Modernization roadmap.
                                15. Refactoring roadmap.

                                Evaluate adoption opportunities for:

                                - Spring AI
                                - LangChain4j
                                - RAG
                                - Vector Search
                                - Kafka
                                - Redis
                                - CQRS
                                - Event Driven Architecture
                                - Kubernetes
                                - OpenTelemetry
                                - Graph Databases

                                Audience:
                                New developers and architects.

                                Length:
                                Approximately 1000 words.

                                Output Sections:

                                Application Purpose
                                Domain Overview
                                Architecture Walkthrough
                                Request Flow
                                Data Flow
                                Module Responsibilities
                                Technology Assessment
                                Technical Debt Analysis
                                Scalability Review
                                Security Review
                                Extensibility Opportunities
                                AI Adoption Opportunities
                                Modernization Roadmap
                                Developer Onboarding Guidance
                                Conclusion

                                ================ APPLICATION CONTEXT ================

                                """);

                prompt.append("Application Name: ")
                                .append(response.applicationKey())
                                .append("\n\n");

                response.subsystems().stream()
                                .limit(24)
                                .forEach(subsystem -> appendBusinessSubsystem(prompt, subsystem));

                prompt.append("\nSubsystem Interactions:\n");

                response.subsystemLinks().stream()
                                .limit(40)
                                .forEach(link -> appendLink(prompt, link));

                return prompt.toString();
        }

        private void appendBusinessSubsystem(StringBuilder prompt, SubsystemDto subsystem) {
                prompt.append("\nSubsystem: ").append(subsystem.name())
                                .append(" (").append(subsystem.id()).append(")\n");
                if (StringUtils.hasText(subsystem.description())) {
                        prompt.append("  - Purpose: ").append(subsystem.description()).append("\n");
                }
                if (subsystem.topPackages() != null && !subsystem.topPackages().isEmpty()) {
                        prompt.append("  - Key packages: ")
                                        .append(String.join(", ", subsystem.topPackages().stream().limit(5).toList()))
                                        .append("\n");
                }
                if (subsystem.centralNodes() != null && !subsystem.centralNodes().isEmpty()) {
                        prompt.append("  - Key classes: ").append(
                                        subsystem.centralNodes().stream().limit(5)
                                                        .map(n -> n.name())
                                                        .collect(Collectors.joining(", ")))
                                        .append("\n");
                }
        }

        private void appendLink(StringBuilder prompt, SubsystemLinkDto link) {
                prompt.append("- ")
                                .append(link.source())
                                .append(" -> ")
                                .append(link.target())
                                .append(": ")
                                .append(link.couplingStrength())
                                .append(", edges=")
                                .append(link.edgeCount())
                                .append("\n");
        }
}

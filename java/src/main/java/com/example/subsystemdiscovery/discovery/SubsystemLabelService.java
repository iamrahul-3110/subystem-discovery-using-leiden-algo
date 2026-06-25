package com.example.subsystemdiscovery.discovery;

import com.example.subsystemdiscovery.algorithm.model.LabelResult;
import com.example.subsystemdiscovery.algorithm.model.SubsystemDraft;
import com.example.subsystemdiscovery.discovery.dto.CentralNodeDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SubsystemLabelService {

    private static final Pattern CAMEL_WORD = Pattern.compile("[A-Z]?[a-z]+|[A-Z]+(?![a-z])|\\d+");
    private static final Set<String> TECHNICAL_SUFFIXES = Set.of(
            "service", "impl", "controller", "mapper", "repository", "repo",
            "entity", "dto", "api", "data", "rest", "grpc", "class", "method"
    );

    /**
     * Generates a heuristic label for the given cluster draft.
     *
     * @param draft the cluster draft produced by {@code ClusterAggregationUtil}
     * @return heuristic {@link LabelResult}; never null
     */
    public LabelResult label(SubsystemDraft draft) {
        return fallbackLabel(draft);
    }


    // =========================================================================
    // Heuristic / fallback labelling
    // =========================================================================

    private LabelResult fallbackLabel(SubsystemDraft draft) {
        List<String> tokens = new ArrayList<>();
        // Use class nodes first, then method nodes for vocabulary
        for (CentralNodeDto node : draft.getClassNodes()) {
            tokens.addAll(splitMeaningfulTokens(node.name()));
        }
        for (CentralNodeDto node : draft.getMethodNodes()) {
            tokens.addAll(splitMeaningfulTokens(node.name()));
        }
        // Fall back to centralNodes if class/method lists are empty
        if (tokens.isEmpty()) {
            for (CentralNodeDto node : draft.getCentralNodes()) {
                tokens.addAll(splitMeaningfulTokens(node.name()));
            }
        }

        String primary     = tokens.stream().findFirst().orElseGet(() -> packageFallback(draft));
        String secondary   = inferSecondaryWord(draft);
        String name        = toTitleCase(primary) + " " + secondary;
        String description = "Groups code related to " + primary.toLowerCase(Locale.ROOT)
                + " responsibilities and closely connected dependencies.";
        return new LabelResult(name, description, 0.5);
    }

    private String packageFallback(SubsystemDraft draft) {
        return draft.getTopPackages().stream()
                .findFirst()
                .map(value -> {
                    String[] parts = value.split("\\.");
                    return parts.length == 0 ? "Subsystem" : parts[parts.length - 1];
                })
                .orElse("Subsystem");
    }

    private String inferSecondaryWord(SubsystemDraft draft) {
        boolean hasController         = draft.getClassNodes().stream()
                .anyMatch(n -> containsIgnoreCase(n.name(), "Controller"));
        boolean hasMapperOrRepository = draft.getClassNodes().stream()
                .anyMatch(n -> containsIgnoreCase(n.name(), "Mapper")
                            || containsIgnoreCase(n.name(), "Repository"));
        boolean hasApi                = !draft.getApiEndpoints().isEmpty();
        // Favour method-call-heavy clusters as business logic
        int methodCallCount           = draft.getRelationSummary().getOrDefault("METHOD_CALL", 0);
        int classDepCount             = draft.getRelationSummary().getOrDefault("CLASS_DEPENDENCY", 0);

        if (hasApi || hasController)            return "Operations";
        if (hasMapperOrRepository)              return "Data";
        if (methodCallCount > classDepCount)    return "Logic";
        return "Management";
    }

    private List<String> splitMeaningfulTokens(String value) {
        List<String> words = new ArrayList<>();
        Matcher matcher = CAMEL_WORD.matcher(value == null ? "" : value);
        while (matcher.find()) {
            String word = matcher.group().toLowerCase(Locale.ROOT);
            if (!TECHNICAL_SUFFIXES.contains(word) && word.length() > 1) {
                words.add(word);
            }
        }
        return words;
    }

    private boolean containsIgnoreCase(String value, String token) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT));
    }

    private String toTitleCase(String value) {
        if (!StringUtils.hasText(value)) return "Subsystem";
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1).toLowerCase(Locale.ROOT);
    }
}

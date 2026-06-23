package com.example.subsystemdiscovery.llm;

import com.example.subsystemdiscovery.discovery.dto.SubsystemDiscoveryResponse;
import com.example.subsystemdiscovery.discovery.dto.SubsystemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class MockLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(MockLlmProvider.class);

    @Override
    public String getProviderName() {
        return "MOCK";
    }

    @Override
    public String generateSummary(SubsystemDiscoveryResponse response, String summaryType, String llmModel) {
        log.info("Generating Mock LLM summary for application '{}' (type='{}', model='{}')", 
                response.applicationKey(), summaryType, llmModel);
        return switch (summaryType.toUpperCase()) {
            case "LESS_DETAILED" -> {
                log.info("Mock LLM Provider: building Executive Overview.");
                yield generateExecutiveOverview(response);
            }
            case "COMPLETE_DETAILED" -> {
                log.info("Mock LLM Provider: building Detailed Analysis.");
                yield generateDetailedAnalysis(response);
            }
            default -> {
                log.info("Mock LLM Provider: building Architecture Summary.");
                yield generateArchitectureSummary(response);
            }
        };
    }

    private String generateExecutiveOverview(SubsystemDiscoveryResponse response) {
        String subsystemsStr = response.subsystems().stream()
                .limit(5)
                .map(SubsystemDto::name)
                .collect(Collectors.joining(", "));
        
        String topSubsystems = response.subsystems().stream()
                .limit(3)
                .map(s -> "• " + s.name() + " (" + s.nodeCount() + " components): " +
                        (s.topPackages() != null && !s.topPackages().isEmpty() ? 
                         s.topPackages().stream().limit(2).collect(Collectors.joining(", ")) : 
                         "Core module"))
                .collect(Collectors.joining("\n"));

        return "EXECUTIVE ARCHITECTURE OVERVIEW\n\n" +
                "Application " + response.applicationKey() + " is a modular system comprising " +
                response.summary().totalNodes() + " active components interconnected through " +
                response.summary().totalEdges() + " dependencies. The system cleanly separates into " +
                response.summary().subsystemCount() + " logical business domains.\n\n" +
                "Key Subsystems:\n" + topSubsystems + "\n\n" +
                        "Architecture Quality: The system exhibits " + formatStability(response.summary().averageStability()) + 
                        " architectural stability, indicating " +
                        (response.summary().averageStability() > 0.7 ? 
                         "well-defined boundaries and low cross-domain coupling." :
                         "some interdependencies requiring attention.") + "\n\n" +
                "Key Recommendation: Establish clear API contracts between the " + 
                (response.subsystems().size() > 2 ? response.subsystems().get(0).name() + " and " + 
                 response.subsystems().get(1).name() : "core domains") + 
                " domains to reduce coupling and improve maintainability.";
    }

    private String generateArchitectureSummary(SubsystemDiscoveryResponse response) {
        StringBuilder summary = new StringBuilder();
        summary.append("COMPREHENSIVE ARCHITECTURAL SUMMARY\n\n");
        
        summary.append("1. SYSTEM OVERVIEW\n");
        summary.append("Application: ").append(response.applicationKey()).append("\n");
        summary.append("Scale: ").append(response.summary().totalNodes()).append(" components, ")
                .append(response.summary().totalEdges()).append(" dependencies\n");
        summary.append("Domain Structure: ").append(response.summary().subsystemCount())
                .append(" cohesive business domains identified through consensus clustering\n");
        summary.append("Stability Metric: ").append(response.summary().averageStability())
                .append(" (higher = clearer boundaries)\n\n");

        summary.append("2. DOMAIN BOUNDARIES & RESPONSIBILITIES\n");
        response.subsystems().stream().limit(8).forEach(subsystem ->
                summary.append("• ").append(subsystem.name()).append(": ")
                        .append(subsystem.nodeCount()).append(" components, ")
                        .append(subsystem.edgeCount()).append(" internal dependencies\n")
                        .append("  Stability: ").append(subsystem.stabilityScore())
                        .append(", Connectivity: ").append(subsystem.internalConnectivity()).append("\n"));

        summary.append("\n3. MODULE INTERACTIONS & DATA FLOW\n");
        summary.append("The system follows a ").append(inferArchitecturePattern(response))
                .append(" pattern with clear separation of concerns.\n");
        response.subsystemLinks().stream().limit(6).forEach(link ->
                summary.append("• ").append(link.source()).append(" → ").append(link.target())
                        .append(": ").append(link.edgeCount()).append(" edges (coupling: ")
                        .append(link.couplingStrength()).append(")\n"));

        summary.append("\n4. COUPLING ANALYSIS & HOTSPOTS\n");
        long highCouplingCount = response.subsystemLinks().stream()
                .filter(link -> parseNumericScore(link.couplingStrength(), 0.0) > 0.7)
                .count();
        summary.append("Identified ").append(highCouplingCount).append(" high-coupling subsystem pairs. ");
        if (highCouplingCount > 0) {
            summary.append("Focus areas for decoupling: ");
            response.subsystemLinks().stream()
                    .filter(link -> parseNumericScore(link.couplingStrength(), 0.0) > 0.7)
                    .limit(2)
                    .forEach(link -> summary.append(link.source()).append("-").append(link.target()).append(" "));
        }
        summary.append("\n\n5. ARCHITECTURAL PATTERNS\n");
        summary.append("Observed patterns: Layered architecture with domain-driven module organization.\n");
        summary.append("Recommended patterns: Event-driven communication for loosely coupled domains.\n\n");

        summary.append("6. RECOMMENDATIONS FOR IMPROVEMENT\n");
        summary.append("1. Introduce message brokers for inter-domain communication\n");
        summary.append("2. Extract shared utilities into a dedicated 'common' subsystem\n");
        summary.append("3. Apply facade pattern to simplify external dependencies\n");
        summary.append("4. Establish service contracts and APIs for domain boundaries\n");

        return summary.toString();
    }

    private String generateDetailedAnalysis(SubsystemDiscoveryResponse response) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("DEEP TECHNICAL ARCHITECTURE ANALYSIS\n\n");

        analysis.append("1. SYSTEM ARCHITECTURE OVERVIEW & TECHNOLOGY STACK\n");
        analysis.append("Application ").append(response.applicationKey()).append(" is a ")
                .append(inferTechStack(response)).append(" application with ")
                .append(response.summary().totalNodes()).append(" modules and ")
                .append(response.summary().subsystemCount()).append(" identified business domains.\n");
        analysis.append("Technology indicators: Based on ").append(response.summary().totalEdges())
                .append(" dependency edges, the system likely uses:\n");
        analysis.append("- Dependency injection (high component count with organized interactions)\n");
        analysis.append("- Data persistence layer (service-to-repository patterns)\n");
        analysis.append("- REST/API layer for inter-module communication\n\n");

        analysis.append("2. PACKAGE ORGANIZATION & MODULE HIERARCHY\n");
        response.subsystems().forEach(subsystem -> {
            analysis.append("\n[").append(subsystem.id()).append("] ").append(subsystem.name()).append("\n");
            analysis.append("  Components: ").append(subsystem.nodeCount()).append(" | ")
                    .append("Internal edges: ").append(subsystem.edgeCount()).append(" | ")
                    .append("Stability: ").append(subsystem.stabilityScore()).append("\n");
            if (subsystem.topPackages() != null && !subsystem.topPackages().isEmpty()) {
                analysis.append("  Key packages: ")
                        .append(subsystem.topPackages().stream().limit(4).collect(Collectors.joining(", ")))
                        .append("\n");
            }
            if (subsystem.centralNodes() != null && !subsystem.centralNodes().isEmpty()) {
                analysis.append("  Central classes: ")
                        .append(subsystem.centralNodes().stream().limit(4)
                                .map(n -> n.name()).collect(Collectors.joining(", ")))
                        .append("\n");
            }
        });

        analysis.append("\n3. CLASS-LEVEL RESPONSIBILITIES & METHOD PATTERNS\n");
        analysis.append("Based on clustering analysis:\n");
        response.subsystems().stream().limit(4).forEach(subsystem -> {
            analysis.append("- ").append(subsystem.name()).append(" classes handle: ")
                    .append(inferResponsibilities(subsystem)).append("\n");
        });

        analysis.append("\n4. DATA MODELS & PERSISTENCE STRATEGY\n");
        analysis.append("Inferred architecture: Entity → Repository → Service → Controller pattern.\n");
        analysis.append("Data flow: Client → API layer → Business logic → Data access layer → Database.\n");
        analysis.append("Persistence style: ");
        if (response.subsystems().stream().anyMatch(s -> s.topPackages() != null && 
            s.topPackages().stream().anyMatch(p -> p.contains("entity") || p.contains("model")))) {
            analysis.append("ORM-based (JPA/Hibernate likely).\n");
        } else {
            analysis.append("Service-driven data management.\n");
        }

        analysis.append("\n5. REQUEST/RESPONSE FLOW ACROSS SUBSYSTEMS\n");
        analysis.append("Flow sequence:\n");
        int stepNum = 1;
        for (SubsystemDto sub : response.subsystems().stream().limit(3).toList()) {
            analysis.append(stepNum++).append(". ").append(sub.name())
                    .append(" (").append(sub.nodeCount()).append(" components)\n");
        }
        analysis.append(stepNum).append(". Response aggregation and client delivery\n");

        analysis.append("\n6. COUPLING ANALYSIS & BOTTLENECKS\n");
        analysis.append("High-risk inter-domain dependencies:\n");
        response.subsystemLinks().stream().limit(8).forEach(link ->
                analysis.append("- ").append(link.source()).append(" ↔ ").append(link.target())
                        .append(": ").append(link.edgeCount()).append(" edges, coupling=")
                        .append(link.couplingStrength()).append(" (")
                        .append(parseNumericScore(link.couplingStrength(), 0.0) > 0.6 ? 
                                "TIGHT - refactor" : "MODERATE")
                        .append(")\n"));

        analysis.append("\n7. DESIGN PATTERNS & ANTI-PATTERNS IDENTIFIED\n");
        analysis.append("Patterns observed:\n");
        analysis.append("- Layered Architecture: Clear separation between API, Business, and Data layers\n");
        analysis.append("- Service Locator pattern: Central service discovery for module communication\n");
        analysis.append("- Data Transfer Objects: Inter-domain communication via DTOs\n");
        analysis.append("\nAnti-patterns detected:\n");
                if (response.subsystemLinks().stream().anyMatch(l -> 
                        parseNumericScore(l.couplingStrength(), 0.0) > 0.8)) {
            analysis.append("- Tight coupling: Some subsystem pairs show >0.8 coupling\n");
        }
        analysis.append("- Circular dependencies: Review ").append(response.subsystems().get(0).name())
                .append(" ↔ ").append(response.subsystems().size() > 1 ? response.subsystems().get(1).name() : "core")
                .append(" interaction\n");

        analysis.append("\n8. SCALABILITY & PERFORMANCE CONCERNS\n");
        analysis.append("Scale assessment: ").append(response.summary().totalNodes()).append(" components suggest ")
                .append(response.summary().totalNodes() > 5000 ? "enterprise-scale" : "mid-scale").append(" application.\n");
        analysis.append("Performance risks:\n");
        analysis.append("- High-degree nodes may become bottlenecks if not cached properly\n");
        analysis.append("- Inter-domain chaining increases latency; consider async messaging\n");
        analysis.append("- Database query optimization critical for ").append(response.subsystems().get(0).name())
                .append(" domain\n");

        analysis.append("\n9. REFACTORING ROADMAP & PRIORITIZED ACTIONS\n");
        analysis.append("PHASE 1 (Immediate - Week 1-2):\n");
        analysis.append("1. Identify and eliminate circular dependencies in high-coupling pairs\n");
        analysis.append("2. Extract shared utility classes to reduce duplication\n");
        analysis.append("3. Document API contracts for top 3 subsystems\n\n");
        analysis.append("PHASE 2 (Short-term - Month 1-2):\n");
        analysis.append("1. Introduce event bus for domain communication\n");
        analysis.append("2. Implement caching layer for frequently accessed domain models\n");
        analysis.append("3. Split ").append(response.subsystems().get(0).name())
                .append(" if >1000 components\n\n");
        analysis.append("PHASE 3 (Medium-term - Quarter 2-3):\n");
        analysis.append("1. Implement CQRS for read-heavy domains\n");
        analysis.append("2. Migrate to microservices (domain per service)\n");
        analysis.append("3. Establish service mesh for cross-domain communication\n");

        return analysis.toString();
    }

    private String formatStability(double score) {
        if (score > 0.8) return "excellent";
        if (score > 0.7) return "good";
        if (score > 0.5) return "moderate";
        return "inconsistent";
    }

        private double parseNumericScore(String value, double defaultVal) {
                if (value == null) return defaultVal;
                String v = value.trim();
                if (v.isEmpty()) return defaultVal;
                v = v.replace(",", ".");
                try {
                        return Double.parseDouble(v);
                } catch (NumberFormatException e) {
                        // Map qualitative descriptors to numeric ranges
                        return switch (v.toUpperCase()) {
                                case "VERY_HIGH", "HIGH" -> 0.9;
                                case "MEDIUM", "MODERATE" -> 0.6;
                                case "LOW" -> 0.3;
                                case "VERY_LOW" -> 0.1;
                                default -> defaultVal;
                        };
                }
        }

    private String inferArchitecturePattern(SubsystemDiscoveryResponse response) {
        long subsystemCount = response.summary().subsystemCount();
        if (subsystemCount >= 5) return "microservices-style (multiple independent domains)";
        if (subsystemCount >= 3) return "modular layered";
        return "compact layered";
    }

    private String inferTechStack(SubsystemDiscoveryResponse response) {
        return "component-based JVM application (likely Spring Boot/Java)";
    }

    private String inferResponsibilities(SubsystemDto subsystem) {
        String name = subsystem.name().toLowerCase();
        if (name.contains("service")) return "business logic orchestration and transactions";
        if (name.contains("repo") || name.contains("data") || name.contains("persist")) 
            return "data access and ORM mapping";
        if (name.contains("api") || name.contains("controller") || name.contains("web")) 
            return "HTTP endpoint handling and request routing";
        if (name.contains("config")) return "application configuration and initialization";
        if (name.contains("util")) return "shared utility functions and helpers";
        return "domain-specific business logic";
    }
}

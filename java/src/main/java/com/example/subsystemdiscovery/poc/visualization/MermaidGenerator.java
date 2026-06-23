package com.example.subsystemdiscovery.poc.visualization;

import com.example.subsystemdiscovery.subsystem.dto.CentralNodeDto;
import com.example.subsystemdiscovery.subsystem.dto.SubsystemDto;
import com.example.subsystemdiscovery.subsystem.dto.SubsystemLinkDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class MermaidGenerator {

    private static final Logger log = LoggerFactory.getLogger(MermaidGenerator.class);

    public String generate(List<SubsystemLinkDto> links, List<SubsystemDto> subsystems) {
        log.info("Generating Mermaid diagram flow chart: {} subsystems, {} links", subsystems.size(), links.size());
        StringBuilder sb = new StringBuilder("flowchart LR\n");

        for (SubsystemDto subsystem : subsystems) {
            String clusterId = mermaidId(subsystem.id());
            String rootId = clusterId + "_root";
            sb.append("    subgraph ").append(clusterId)
                    .append("[\"").append(escape(subsystem.name())).append("\"]\n");
            sb.append("        ").append(rootId)
                    .append("[\"").append(escape(subsystem.name()))
                    .append("<br/>").append(subsystem.nodeCount()).append(" nodes")
                    .append("<br/>stability ").append(subsystem.stabilityScore())
                    .append("\"]\n");

            subsystem.centralNodes().stream()
                    .limit(3)
                    .forEach(node -> appendCentralNode(sb, rootId, clusterId, node));
            sb.append("    end\n");
        }

        for (SubsystemLinkDto link : links) {
            String source = mermaidId(link.source()) + "_root";
            String target = mermaidId(link.target()) + "_root";
            String arrow = "HIGH".equalsIgnoreCase(link.couplingStrength()) ? " ==>" :
                    "LOW".equalsIgnoreCase(link.couplingStrength()) ? " -.->" : " -->";
            sb.append("    ").append(source)
                    .append(arrow)
                    .append("|").append(link.couplingStrength()).append(" / ")
                    .append(link.edgeCount()).append(" edges| ")
                    .append(target)
                    .append("\n");
        }

        sb.append("\n")
                .append("    classDef root fill:#eff6ff,stroke:#1d4ed8,stroke-width:1.5px,color:#172554;\n")
                .append("    classDef node fill:#ffffff,stroke:#94a3b8,color:#0f172a;\n");

        for (SubsystemDto subsystem : subsystems) {
            String clusterId = mermaidId(subsystem.id());
            sb.append("    class ").append(clusterId).append("_root root;\n");
            subsystem.centralNodes().stream().limit(3).forEach(node ->
                    sb.append("    class ").append(clusterId).append("_n").append(Math.abs(node.id().hashCode()))
                            .append(" node;\n"));
        }

        log.info("Successfully generated Mermaid markup (length={}).", sb.length());
        return sb.toString();
    }

    private void appendCentralNode(StringBuilder sb, String rootId, String clusterId, CentralNodeDto node) {
        String nodeId = clusterId + "_n" + Math.abs(node.id().hashCode());
        sb.append("        ").append(nodeId)
                .append("[\"").append(escape(node.name())).append("\"]\n");
        sb.append("        ").append(rootId).append(" --- ").append(nodeId).append("\n");
    }

    private String mermaidId(String raw) {
        String value = raw == null ? "cluster" : raw.toLowerCase(Locale.ROOT);
        return "m_" + value.replaceAll("[^a-z0-9_]+", "_");
    }

    private String escape(String raw) {
        return raw == null ? "" : raw.replace("\"", "'");
    }
}

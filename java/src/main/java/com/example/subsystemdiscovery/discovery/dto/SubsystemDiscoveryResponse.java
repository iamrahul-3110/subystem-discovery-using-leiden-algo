package com.example.subsystemdiscovery.discovery.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

public record SubsystemDiscoveryResponse(
        @JsonIgnore Long discoveryRunId,
        @JsonIgnore Long applicationId,
        @JsonIgnore String applicationKey,
        @JsonIgnore AlgorithmInfoDto algorithm,
        SummaryDto summary,
        List<SubsystemDto> subsystems,
        List<SubsystemLinkDto> subsystemLinks,
        @JsonIgnore List<NodeAssignmentDto> nodeAssignments
) {
}


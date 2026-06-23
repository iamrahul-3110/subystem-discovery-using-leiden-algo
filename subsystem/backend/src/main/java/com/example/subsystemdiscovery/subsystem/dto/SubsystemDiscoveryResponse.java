package com.example.subsystemdiscovery.subsystem.dto;

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


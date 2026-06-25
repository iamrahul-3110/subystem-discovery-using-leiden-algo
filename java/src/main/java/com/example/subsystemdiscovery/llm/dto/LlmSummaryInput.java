package com.example.subsystemdiscovery.llm.dto;
import com.example.subsystemdiscovery.discovery.dto.SubsystemDto;
import com.example.subsystemdiscovery.discovery.dto.SubsystemLinkDto;

import java.util.List;

public record LlmSummaryInput(
        int totalSubsystems,
        double avgStability,
        List<SubsystemDto> subsystems,
        List<SubsystemLinkDto> subsystemLinks
) {
}

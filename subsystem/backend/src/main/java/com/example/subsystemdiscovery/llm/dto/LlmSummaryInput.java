package com.example.subsystemdiscovery.llm.dto;
import com.example.subsystemdiscovery.subsystem.dto.SubsystemDto;
import com.example.subsystemdiscovery.subsystem.dto.SubsystemLinkDto;

import java.util.List;

public record LlmSummaryInput(
        int totalSubsystems,
        double avgStability,
        List<SubsystemDto> subsystems,
        List<SubsystemLinkDto> subsystemLinks
) {
}

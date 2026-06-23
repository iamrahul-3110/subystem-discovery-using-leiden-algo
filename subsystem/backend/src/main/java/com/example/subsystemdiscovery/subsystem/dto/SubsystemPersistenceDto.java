package com.example.subsystemdiscovery.subsystem.dto;

import java.util.List;

public record SubsystemPersistenceDto(
        List<SubsystemDto> subsystems,
        List<SubsystemLinkDto> subsystemLinks
) {
}

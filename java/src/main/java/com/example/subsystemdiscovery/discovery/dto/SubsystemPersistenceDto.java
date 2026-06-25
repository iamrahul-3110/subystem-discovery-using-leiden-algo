package com.example.subsystemdiscovery.discovery.dto;

import java.util.List;

public record SubsystemPersistenceDto(
        List<SubsystemDto> subsystems,
        List<SubsystemLinkDto> subsystemLinks
) {
}

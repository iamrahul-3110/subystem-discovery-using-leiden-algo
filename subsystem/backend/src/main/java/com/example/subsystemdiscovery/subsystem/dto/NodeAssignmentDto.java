package com.example.subsystemdiscovery.subsystem.dto;

public record NodeAssignmentDto(
                Long nodeId,
                String subsystemId,
                double membershipScore) {
}

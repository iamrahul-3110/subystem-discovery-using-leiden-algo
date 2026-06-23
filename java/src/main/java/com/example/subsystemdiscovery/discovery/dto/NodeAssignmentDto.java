package com.example.subsystemdiscovery.discovery.dto;

public record NodeAssignmentDto(
                Long nodeId,
                String subsystemId,
                double membershipScore) {
}

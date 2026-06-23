package com.example.subsystemdiscovery.repository.entity;

/**
 * Metadata retrieved from joining tb_node_history_master and tb_application_master
 * by analysisTime.
 */
public record ApplicationMetadata(Long applicationId, String applicationKey) {
}

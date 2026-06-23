package com.example.subsystemdiscovery.algorithm.model;

import com.example.subsystemdiscovery.discovery.dto.NodeAssignmentDto;
import com.example.subsystemdiscovery.discovery.dto.SubsystemLinkDto;
import com.example.subsystemdiscovery.algorithm.model.SubsystemDraft;

import java.util.ArrayList;
import java.util.List;

public class ClusterAggregation {
    private List<SubsystemDraft> subsystemDrafts = new ArrayList<>();
    private List<SubsystemLinkDto> subsystemLinks = new ArrayList<>();
    private List<NodeAssignmentDto> nodeAssignments = new ArrayList<>();

    public List<SubsystemDraft> getSubsystemDrafts() {
        return subsystemDrafts;
    }

    public void setSubsystemDrafts(List<SubsystemDraft> subsystemDrafts) {
        this.subsystemDrafts = subsystemDrafts;
    }

    public List<SubsystemLinkDto> getSubsystemLinks() {
        return subsystemLinks;
    }

    public void setSubsystemLinks(List<SubsystemLinkDto> subsystemLinks) {
        this.subsystemLinks = subsystemLinks;
    }

    public List<NodeAssignmentDto> getNodeAssignments() {
        return nodeAssignments;
    }

    public void setNodeAssignments(List<NodeAssignmentDto> nodeAssignments) {
        this.nodeAssignments = nodeAssignments;
    }
}

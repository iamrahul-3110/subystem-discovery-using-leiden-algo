package com.example.subsystemdiscovery.repository;

import com.example.subsystemdiscovery.repository.entity.NodeDetailRow;
import com.example.subsystemdiscovery.repository.entity.NodeLinkRow;
import com.example.subsystemdiscovery.repository.entity.NodeRow;
import com.example.subsystemdiscovery.repository.entity.ApplicationMetadata;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis mapper interface for querying the three history tables:
 * <ul>
 *   <li>{@code tb_node_history}         — nodes per analysis snapshot</li>
 *   <li>{@code tb_node_detail_history}  — split-level name details per node</li>
 *   <li>{@code tb_node_relation_history}— call-graph edges per snapshot</li>
 * </ul>
 *
 * <p>Every query is scoped to a single analysis snapshot identified by the
 * composite key {@code (applicationId, analysisTime)}.
 *
 * <p>SQL is defined in {@code src/main/resources/mapper/TbNodeHistoryMapper.xml}.
 */
@Mapper
public interface TbNodeHistoryMapper {

    /**
     * Retrieves the application metadata (ID and key) for a given analysis time.
     *
     * @param analysisTime snapshot key
     * @return application metadata matching the analysis time
     */
    ApplicationMetadata selectApplicationMetadata(@Param("analysisTime") String analysisTime);

    /**
     * Loads all nodes for the given application + analysis snapshot
     * from {@code tb_node_history}.
     *
     * @param applicationId numeric application ID
     * @param analysisTime  snapshot key
     * @return ordered list of node rows
     */
    List<NodeRow> selectNodes(
            @Param("applicationId") Long applicationId,
            @Param("analysisTime")  String analysisTime);

    /**
     * Loads all node-detail rows for the given application + analysis snapshot
     * from {@code tb_node_detail_history}.
     *
     * @param applicationId numeric application ID
     * @param analysisTime  snapshot key
     * @return list of detail rows ordered by node_id, split_node_level
     */
    List<NodeDetailRow> selectNodeDetails(
            @Param("applicationId") Long applicationId,
            @Param("analysisTime")  String analysisTime);

    /**
     * Loads all call-graph edges for the given application + analysis snapshot
     * from {@code tb_node_relation_history}.
     *
     * @param applicationId numeric application ID
     * @param analysisTime  snapshot key
     * @return list of directed edge rows ordered by relation_id
     */
    List<NodeLinkRow> selectLinks(
            @Param("applicationId") Long applicationId,
            @Param("analysisTime")  String analysisTime);
}


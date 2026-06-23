package com.example.subsystemdiscovery.poc.generator;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PocNodeMapper {

    @Insert("MERGE INTO tb_application_master (application_id, application_name, application_key) " +
            "KEY(application_id) VALUES (#{appId}, #{appName}, #{appKey})")
    void insertAppMaster(@Param("appId") Long appId, @Param("appName") String appName, @Param("appKey") String appKey);

    @Insert("INSERT INTO tb_node_history (application_id, analysis_time, node_id, node_name, node_type, use_yn) " +
            "VALUES (#{appId}, CAST(#{analysisTime} AS TIMESTAMP), #{nodeId}, #{nodeName}, #{nodeType}, TRUE)")
    void insertNodeHistory(@Param("appId") Long appId, @Param("analysisTime") String analysisTime,
                           @Param("nodeId") Long nodeId, @Param("nodeName") String nodeName,
                           @Param("nodeType") String nodeType);

    @Insert("INSERT INTO tb_node_detail_history (application_id, analysis_time, node_id, split_node_level, split_node_name, split_node_type) " +
            "VALUES (#{appId}, CAST(#{analysisTime} AS TIMESTAMP), #{nodeId}, #{level}, #{name}, #{type})")
    void insertNodeDetail(@Param("appId") Long appId, @Param("analysisTime") String analysisTime,
                          @Param("nodeId") Long nodeId, @Param("level") int level,
                          @Param("name") String name, @Param("type") String type);

    @Insert("INSERT INTO tb_node_relation_history (application_id, analysis_time, relation_id, source_node_id, target_node_id, relation_type) " +
            "VALUES (#{appId}, CAST(#{analysisTime} AS TIMESTAMP), #{relationId}, #{sourceId}, #{targetId}, #{relationType})")
    void insertNodeRelation(@Param("appId") Long appId, @Param("analysisTime") String analysisTime,
                            @Param("relationId") Long relationId,
                            @Param("sourceId") Long sourceId, @Param("targetId") Long targetId,
                            @Param("relationType") String relationType);
}

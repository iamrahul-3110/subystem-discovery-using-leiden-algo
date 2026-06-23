-- Sample Analyzer-style graph data for local H2 testing.
-- Paste this script into http://localhost:8080/h2-console and run it.
--
-- H2 console connection:
-- JDBC URL: jdbc:h2:mem:subsystem_discovery
-- User Name: sa
-- Password: leave blank
--
-- Then open the UI, choose Database, applicationId=14, and click Run.

delete from tb_node_relation where application_id = 14;
delete from tb_node_detail where node_id between 140001 and 140024;
delete from tb_node where application_id = 14 and node_id between 140001 and 140024;

insert into tb_node (
    application_id, node_id, analysis_mode, node_type, node_name, hash, use_yn, create_user_id, create_date, update_user_id, update_date
) values
(14, 140001, 'S', 'JAVA', 'com.example.analyzer.auth.api.AuthController.login(java.lang.String,java.lang.String)', 'sample-hash-140001', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),
(14, 140002, 'S', 'JAVA', 'com.example.analyzer.auth.app.AuthService.authenticate(java.lang.String,java.lang.String)', 'sample-hash-140002', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),
(14, 140003, 'S', 'JAVA', 'com.example.analyzer.auth.token.JwtTokenService.createToken(com.example.analyzer.auth.model.UserAccount)', 'sample-hash-140003', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),
(14, 140004, 'S', 'JAVA', 'com.example.analyzer.auth.data.UserRepository.findByUsername(java.lang.String)', 'sample-hash-140004', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),
(14, 140005, 'S', 'JAVA', 'com.example.analyzer.auth.policy.PasswordPolicy.validate(java.lang.String)', 'sample-hash-140005', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),

(14, 140006, 'S', 'JAVA', 'com.example.analyzer.project.api.ProjectController.listProjects(java.lang.Long)', 'sample-hash-140006', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),
(14, 140007, 'S', 'JAVA', 'com.example.analyzer.project.app.ProjectService.findProjects(java.lang.Long)', 'sample-hash-140007', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),
(14, 140008, 'S', 'JAVA', 'com.example.analyzer.project.data.ProjectRepository.selectProjects(java.lang.Long)', 'sample-hash-140008', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),
(14, 140009, 'S', 'JAVA', 'com.example.analyzer.project.member.ProjectMemberService.attachMembers(java.lang.Long)', 'sample-hash-140009', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),
(14, 140010, 'S', 'JAVA', 'com.example.analyzer.project.security.ProjectPermissionService.resolveAccess(java.lang.Long)', 'sample-hash-140010', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),

(14, 140011, 'S', 'JAVA', 'com.example.analyzer.analysis.api.RepositoryAnalysisController.startAnalysis(java.lang.Long)', 'sample-hash-140011', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),
(14, 140012, 'S', 'JAVA', 'com.example.analyzer.analysis.app.RepositoryAnalysisService.analyzeRepository(java.lang.Long)', 'sample-hash-140012', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),
(14, 140013, 'S', 'JAVA', 'com.example.analyzer.analysis.scan.SourceScanner.scan(java.lang.String)', 'sample-hash-140013', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),
(14, 140014, 'S', 'JAVA', 'com.example.analyzer.analysis.callgraph.CallGraphBuilder.buildCallGraph(java.util.List)', 'sample-hash-140014', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),
(14, 140015, 'S', 'JAVA', 'com.example.analyzer.analysis.writer.NodeRelationWriter.saveRelations(java.util.List)', 'sample-hash-140015', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),

(14, 140016, 'S', 'JAVA', 'com.example.analyzer.codebot.api.CodeBotController.generateAnswer(java.lang.Long)', 'sample-hash-140016', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),
(14, 140017, 'S', 'JAVA', 'com.example.analyzer.codebot.app.CodeBotService.ask(java.lang.Long,java.lang.String)', 'sample-hash-140017', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),
(14, 140018, 'S', 'JAVA', 'com.example.analyzer.codebot.prompt.PromptBuilder.build(java.lang.String)', 'sample-hash-140018', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),
(14, 140019, 'S', 'JAVA', 'com.example.analyzer.codebot.llm.LlmClient.complete(java.lang.String)', 'sample-hash-140019', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),
(14, 140020, 'S', 'JAVA', 'com.example.analyzer.codebot.history.CodeBotHistoryRepository.save(java.lang.String)', 'sample-hash-140020', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),

(14, 140021, 'S', 'JAVA', 'com.example.analyzer.admin.role.api.RoleController.updateRole(java.lang.Long)', 'sample-hash-140021', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),
(14, 140022, 'S', 'JAVA', 'com.example.analyzer.admin.role.app.RoleService.updateRole(java.lang.Long)', 'sample-hash-140022', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),
(14, 140023, 'S', 'JAVA', 'com.example.analyzer.admin.role.data.RoleRepository.save(java.lang.Long)', 'sample-hash-140023', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp),
(14, 140024, 'S', 'JAVA', 'com.example.analyzer.common.audit.AuditTrailService.recordChange(java.lang.String)', 'sample-hash-140024', 'Y', 'SYSTEM', current_timestamp, '', current_timestamp);

insert into tb_node_detail (
    node_id, split_node_level, split_node_name, split_node_type, create_user_id, create_date, update_user_id, update_date
) values
(140001, 0, 'login(java.lang.String,java.lang.String)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140001, 1, 'AuthController', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140001, 2, 'com.example.analyzer.auth.api', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),
(140002, 0, 'authenticate(java.lang.String,java.lang.String)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140002, 1, 'AuthService', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140002, 2, 'com.example.analyzer.auth.app', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),
(140003, 0, 'createToken(com.example.analyzer.auth.model.UserAccount)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140003, 1, 'JwtTokenService', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140003, 2, 'com.example.analyzer.auth.token', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),
(140004, 0, 'findByUsername(java.lang.String)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140004, 1, 'UserRepository', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140004, 2, 'com.example.analyzer.auth.data', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),
(140005, 0, 'validate(java.lang.String)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140005, 1, 'PasswordPolicy', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140005, 2, 'com.example.analyzer.auth.policy', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),

(140006, 0, 'listProjects(java.lang.Long)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140006, 1, 'ProjectController', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140006, 2, 'com.example.analyzer.project.api', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),
(140007, 0, 'findProjects(java.lang.Long)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140007, 1, 'ProjectService', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140007, 2, 'com.example.analyzer.project.app', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),
(140008, 0, 'selectProjects(java.lang.Long)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140008, 1, 'ProjectRepository', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140008, 2, 'com.example.analyzer.project.data', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),
(140009, 0, 'attachMembers(java.lang.Long)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140009, 1, 'ProjectMemberService', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140009, 2, 'com.example.analyzer.project.member', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),
(140010, 0, 'resolveAccess(java.lang.Long)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140010, 1, 'ProjectPermissionService', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140010, 2, 'com.example.analyzer.project.security', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),

(140011, 0, 'startAnalysis(java.lang.Long)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140011, 1, 'RepositoryAnalysisController', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140011, 2, 'com.example.analyzer.analysis.api', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),
(140012, 0, 'analyzeRepository(java.lang.Long)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140012, 1, 'RepositoryAnalysisService', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140012, 2, 'com.example.analyzer.analysis.app', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),
(140013, 0, 'scan(java.lang.String)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140013, 1, 'SourceScanner', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140013, 2, 'com.example.analyzer.analysis.scan', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),
(140014, 0, 'buildCallGraph(java.util.List)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140014, 1, 'CallGraphBuilder', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140014, 2, 'com.example.analyzer.analysis.callgraph', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),
(140015, 0, 'saveRelations(java.util.List)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140015, 1, 'NodeRelationWriter', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140015, 2, 'com.example.analyzer.analysis.writer', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),

(140016, 0, 'generateAnswer(java.lang.Long)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140016, 1, 'CodeBotController', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140016, 2, 'com.example.analyzer.codebot.api', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),
(140017, 0, 'ask(java.lang.Long,java.lang.String)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140017, 1, 'CodeBotService', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140017, 2, 'com.example.analyzer.codebot.app', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),
(140018, 0, 'build(java.lang.String)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140018, 1, 'PromptBuilder', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140018, 2, 'com.example.analyzer.codebot.prompt', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),
(140019, 0, 'complete(java.lang.String)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140019, 1, 'LlmClient', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140019, 2, 'com.example.analyzer.codebot.llm', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),
(140020, 0, 'save(java.lang.String)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140020, 1, 'CodeBotHistoryRepository', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140020, 2, 'com.example.analyzer.codebot.history', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),

(140021, 0, 'updateRole(java.lang.Long)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140021, 1, 'RoleController', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140021, 2, 'com.example.analyzer.admin.role.api', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),
(140022, 0, 'updateRole(java.lang.Long)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140022, 1, 'RoleService', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140022, 2, 'com.example.analyzer.admin.role.app', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),
(140023, 0, 'save(java.lang.Long)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140023, 1, 'RoleRepository', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140023, 2, 'com.example.analyzer.admin.role.data', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp),
(140024, 0, 'recordChange(java.lang.String)', 'METHOD', 'SYSTEM', current_timestamp, '', current_timestamp),
(140024, 1, 'AuditTrailService', 'CLASS', 'SYSTEM', current_timestamp, '', current_timestamp),
(140024, 2, 'com.example.analyzer.common.audit', 'PACKAGE', 'SYSTEM', current_timestamp, '', current_timestamp);

insert into tb_node_relation (
    application_id, relation_id, node_id, parent_node_id, use_yn
) values
-- Auth subsystem
(14, 141001, 140002, 140001, 'Y'),
(14, 141002, 140003, 140002, 'Y'),
(14, 141003, 140004, 140002, 'Y'),
(14, 141004, 140005, 140002, 'Y'),
(14, 141005, 140003, 140004, 'Y'),

-- Project subsystem
(14, 141006, 140007, 140006, 'Y'),
(14, 141007, 140008, 140007, 'Y'),
(14, 141008, 140009, 140007, 'Y'),
(14, 141009, 140010, 140007, 'Y'),
(14, 141010, 140009, 140010, 'Y'),

-- Repository analysis subsystem
(14, 141011, 140012, 140011, 'Y'),
(14, 141012, 140013, 140012, 'Y'),
(14, 141013, 140014, 140012, 'Y'),
(14, 141014, 140015, 140014, 'Y'),
(14, 141015, 140015, 140013, 'Y'),

-- Codebot subsystem
(14, 141016, 140017, 140016, 'Y'),
(14, 141017, 140018, 140017, 'Y'),
(14, 141018, 140019, 140017, 'Y'),
(14, 141019, 140020, 140017, 'Y'),
(14, 141020, 140020, 140019, 'Y'),

-- Admin subsystem
(14, 141021, 140022, 140021, 'Y'),
(14, 141022, 140023, 140022, 'Y'),
(14, 141023, 140024, 140022, 'Y'),
(14, 141024, 140024, 140023, 'Y'),

-- Light cross-subsystem coupling
(14, 141025, 140002, 140010, 'Y'),
(14, 141026, 140010, 140006, 'Y'),
(14, 141027, 140012, 140017, 'Y'),
(14, 141028, 140015, 140012, 'Y'),
(14, 141029, 140024, 140022, 'Y'),
(14, 141030, 140024, 140012, 'Y');

select 'tb_node' as table_name, count(*) as row_count from tb_node where application_id = 14
union all
select 'tb_node_detail', count(*) from tb_node_detail where node_id between 140001 and 140024
union all
select 'tb_node_relation', count(*) from tb_node_relation where application_id = 14;

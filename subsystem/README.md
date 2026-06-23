# Subsystem Discovery DB-Only Movable Code

This bundle contains the code you can move into a real Spring Boot application without the hardcoded demo graph catalog.

## What To Copy

Copy these folders into the target backend source tree, then adjust the Java package if your application does not use `com.example.subsystemdiscovery`.

```text
backend/src/main/java/com/example/subsystemdiscovery/api
backend/src/main/java/com/example/subsystemdiscovery/config
backend/src/main/java/com/example/subsystemdiscovery/domain
backend/src/main/java/com/example/subsystemdiscovery/dto
backend/src/main/java/com/example/subsystemdiscovery/repository
backend/src/main/java/com/example/subsystemdiscovery/service
```

If the target app already has a Spring Boot main class, do not copy `SubsystemDiscoveryApplication.java`.

Copy the frontend page if you want the standalone UI:

```text
frontend/static/index.html
```

In a Spring Boot app this normally goes to:

```text
src/main/resources/static/index.html
```

## Dependencies

Add the dependencies from:

```text
backend/pom-dependencies.xml
```

Skip dependencies already present in your target app.

## Database

No schema migration is required if these PostgreSQL tables already exist:

```text
tb_node
tb_node_detail
tb_node_relation
```

The repository adapter reads:

```text
tb_node + tb_node_detail -> METHOD nodes grouped under CLASS and PACKAGE nodes
tb_node_relation         -> CALL_GRAPH links between METHOD nodes
```

The default query filters graph rows by `application_id`.

Important: the table schemas you provided do not contain an `application_key` column. The movable code accepts and returns `applicationKey`, but table filtering is by `application_id`. If your actual app stores `application_key` in another master table, validate the `(applicationId, applicationKey)` pair before calling discovery, or add a join/exists check in `TbNodeRelationGraphInputRepository`.

## API Endpoints

```text
POST /api/analysis/{applicationId}/subsystems/discover
POST /api/analysis/{applicationId}/subsystems/leiden-input
POST /api/subsystems/transform/leiden-input
```

Example request:

```json
{
  "applicationId": 14,
  "applicationKey": "your-app-key",
  "graphTypes": ["CALL_GRAPH"],
  "runs": 5,
  "consensusThreshold": 0.65,
  "resolution": 1.0,
  "useLlmLabels": false,
  "graphs": null
}
```

## PostgreSQL Config

Use the example in:

```text
config/application-postgres-example.yml
```

Set `spring.sql.init.mode=never` in the real app because your PostgreSQL schema already exists.

## Local H2 Test Data

The `sql` folder includes optional local-only reference files:

```text
sql/schema-reference.sql
sql/sample-h2-analyzer-data.sql
```

Do not run these against production PostgreSQL.

## Removed On Purpose

This bundle does not include:

```text
DemoGraphCatalog.java
hardcoded example endpoints
synthetic graph endpoints
```

The UI and controller are database-first.

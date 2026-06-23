# Subsystem Discovery Engine — Complete Developer Guide

> **For new developers:** This guide explains every class, what it does, and how they all work together — in plain language. No prior knowledge of this codebase required.

---

## Table of Contents

1. [What Does This Application Do?](#1-what-does-this-application-do)
2. [Quick Start](#2-quick-start)
3. [Project Structure](#3-project-structure)
4. [How the Application Works — The Full Pipeline](#4-how-the-application-works--the-full-pipeline)
5. [Every Class Explained](#5-every-class-explained)
   - [Entry Point](#-entry-point)
   - [API Layer](#-api-layer)
   - [Configuration](#-configuration)
   - [Service Layer](#-service-layer)
   - [Domain Model](#-domain-model)
   - [Data Transfer Objects (DTOs)](#-data-transfer-objects-dtos)
   - [Repository Layer](#-repository-layer)
6. [Configuration Reference](#6-configuration-reference)
7. [REST API Reference](#7-rest-api-reference)
8. [Input & Output Formats](#8-input--output-formats)
9. [Interactive Architecture Visualizer](#9-interactive-architecture-visualizer)

---

## 1. What Does This Application Do?

Imagine you have a large Java backend with hundreds of classes calling each other in complex ways. It is very hard to understand which classes belong together as a logical "module" or "service domain". This application **automatically figures that out** for you.

It takes a **call graph** (a map of which Java class calls which other class) and uses a mathematical algorithm called **Leiden** to group the classes into clusters. Each cluster is a **subsystem** — a group of classes that work closely together. It then optionally calls an **LLM (AI)** to give each subsystem a meaningful business name like "User Management" or "Payment Processing".

**Think of it like this:**
- Input: a big messy pile of Java classes and their connections
- Output: a clean map showing "these 24 classes form the User Management subsystem, these 18 form the Order Processing subsystem" etc.

---

## 2. Quick Start

**Requirements:** Java 17+, Maven

```powershell
# Navigate to the java project
cd C:\Project\subsystem-discovery-implementation\java

# Run the application
mvn clean spring-boot:run
```

The application starts on **port 8081** with context path `/codeanalyzer/server`.

| URL | Purpose |
|---|---|
| `http://localhost:8081/codeanalyzer/server` | Main frontend UI |
| `http://localhost:8081/codeanalyzer/server/h2-console` | H2 database console (for local dev) |

H2 console settings: JDBC URL `jdbc:h2:mem:subsystem_discovery`, user `sa`, password empty.

If Maven is not installed:
```powershell
winget install Apache.Maven
# Reopen PowerShell, then verify:
mvn -v
java -version
```

---

## 3. Project Structure

```
subsystem-discovery-implementation/
│
├── README.md                          ← This file
├── subsystem-architecture-visualizer.html  ← Interactive visual walkthrough
├── postman/                           ← Postman collections for testing
│
├── java/                              ← THE MAIN APPLICATION (run this)
│   ├── pom.xml                        ← Maven dependencies
│   └── src/main/
│       ├── java/com/example/subsystemdiscovery/
│       │   ├── SubsystemDiscoveryApplication.java   ← App entry point
│       │   ├── api/                   ← REST controllers & error handling
│       │   ├── config/                ← Configuration classes
│       │   ├── domain/                ← Internal data models
│       │   ├── dto/                   ← Request/response data shapes
│       │   ├── repository/            ← Database access
│       │   └── service/               ← All the business logic
│       └── resources/
│           ├── application.yml        ← App configuration
│           ├── schema.sql             ← Creates H2 demo tables
│           ├── mapper/                ← MyBatis SQL XML files
│           └── static/index.html      ← Frontend web UI
│
└── movable-code/                      ← Reference/portable version of the backend
    └── subsystem-discovery-db-only/
        ├── backend/                   ← Standalone Spring Boot version
        └── config/
            └── application-postgres-example.yml  ← Production PostgreSQL config
```

> **Note for new developers:** The `java/` folder is the runnable application. The `movable-code/` folder is a portable reference copy — same logic, packaged separately for integration into a different project (e.g. the Analyzer server).

---

## 4. How the Application Works — The Full Pipeline

When you call the discovery API, this is what happens **step by step**:

```
1. HTTP Request arrives
        ↓
2. SubsystemDiscoveryController  — validates the request
        ↓
3. SubsystemDiscoveryService     — orchestrates the whole process
        ↓
4. GraphInputCollector           — fetches raw graph data (DB or inline JSON)
        ↓
5. WeightedGraphBuilder          — converts raw nodes/links into a weighted graph
        ↓
6. LeidenClusteringService       — groups nodes into clusters (runs algorithm N times)
        ↓
7. ClusterService                — enriches each cluster with stats, packages, API endpoints
        ↓
8. SubsystemLabelService         — gives each cluster a human-readable name
        ↓
9. SubsystemDiscoveryResponse    — sent back to the caller as JSON
```

Each step is handled by a different Java class. The sections below explain every one of them.

---

## 5. Every Class Explained

---

### 📦 Entry Point

---

#### `SubsystemDiscoveryApplication.java`

**What it does:** This is the very first class that runs when you start the application. It is the "main" class for the Spring Boot app.

**In simple terms:** Think of this as the "on button" for the application. When you run `mvn spring-boot:run`, Java finds this class and calls its `main()` method, which boots the entire Spring framework — setting up the web server, connecting to the database, and wiring all the services together.

**Key things it does:**
- `@SpringBootApplication` — tells Spring to scan all classes in the package and set everything up automatically
- `@ConfigurationPropertiesScan` — tells Spring to also look for configuration classes (like `LlmProperties`) and load them from `application.yml`
- Starts the embedded Tomcat web server on port 8081

```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class SubsystemDiscoveryApplication {
    public static void main(String[] args) {
        SpringApplication.run(SubsystemDiscoveryApplication.class, args);
    }
}
```

---

### 🌐 API Layer

These classes live in the `api/` package. They handle incoming HTTP requests.

---

#### `SubsystemDiscoveryController.java`

**What it does:** This is the "front door" of the application. It receives HTTP requests from clients (browser, Postman, curl, other services) and routes them to the right service method.

**In simple terms:** Think of it like a receptionist at a company. A request comes in, the receptionist checks what it is, and forwards it to the right department. The controller does not do any real work itself — it just delegates.

**Annotations it uses:**
- `@RestController` — tells Spring this class handles HTTP requests and returns JSON
- `@PostMapping` / `@GetMapping` — maps specific URLs to specific Java methods
- `@PathVariable` — pulls a value out of the URL (like `{applicationId}`)
- `@RequestParam` — pulls a value out of the query string (like `?runs=20`)
- `@Valid @RequestBody` — reads and validates the JSON request body

**Endpoints it exposes:**

| Method | URL Pattern | What It Does |
|---|---|---|
| `POST` | `/api/codeanalyzer/analysis/{applicationId}/subsystem/discover` | Main endpoint — loads graph from DB and discovers subsystems |
| `POST` | `/api/analysis/{applicationId}/subsystems/discover` | Same as above — legacy URL kept for backward compatibility |
| `POST` | `/api/codeanalyzer/analysis/{applicationId}/subsystem/leiden-input` | Shows the normalised graph that would be fed into the algorithm (useful for debugging) |
| `POST` | `/api/codeanalyzer/subsystems/transform/leiden-input` | Transforms inline JSON graph into Leiden input format |
| `POST` | `/api/subsystems/transform/leiden-input` | Same — legacy URL |
| `POST` | `/api/subsystems/discover` | Discover on inline JSON graph (no DB) |
| `GET` | `/api/subsystems/examples` | Lists built-in demo graphs |
| `GET` | `/api/subsystems/examples/{key}/raw-graph` | Returns raw nodes+edges for a demo graph |
| `GET` | `/api/subsystems/examples/{key}/leiden-input` | Returns Leiden input for a demo graph |
| `POST` | `/api/subsystems/examples/{key}/discover` | Runs discovery on a demo graph |
| `GET` | `/api/subsystems/generated/{nodeCount}/raw-graph` | Returns a randomly generated graph |
| `GET` | `/api/subsystems/generated/{nodeCount}/leiden-input` | Leiden input for a generated graph |
| `POST` | `/api/subsystems/generated/{nodeCount}/discover` | Discover on a generated graph |

**Smart run scaling for large generated graphs:**
- 5000+ nodes → capped at 1 run (too slow otherwise)
- 1000–4999 nodes → capped at 5 runs
- Under 1000 nodes → uses the requested number of runs

---

#### `ApiExceptionHandler.java`

**What it does:** Catches errors that happen anywhere in the application and converts them into clean JSON error responses instead of letting ugly Java stack traces reach the caller.

**In simple terms:** Imagine a safety net. If anything goes wrong during processing, this class catches the exception and returns a neat JSON message explaining what went wrong.

**What errors it handles:**
- `IllegalArgumentException` and `IllegalStateException` → returns HTTP 400 (Bad Request) — these mean the caller sent invalid input
- Any other `Exception` → returns HTTP 500 (Internal Server Error) — unexpected failures

**Error response shape:**
```json
{
  "timestamp": "2026-06-01T05:30:00Z",
  "status": 400,
  "error": "IllegalArgumentException",
  "message": "applicationId is required when inline graph JSON is not provided"
}
```

---

### ⚙️ Configuration

These classes live in the `config/` package. They manage application settings.

---

#### `LlmProperties.java`

**What it does:** Holds all the configuration settings for the LLM (AI) integration — the API URL, authentication key, and request parameters. It reads these values from `application.yml`.

**In simple terms:** Think of this as a settings class. Instead of hardcoding the API URL or auth key in your code, you put them in `application.yml`, and Spring automatically reads them into this class when the app starts.

**Properties it manages (all under `subsystem.llm.*` in yml):**

| Property | What It Does |
|---|---|
| `enabled` | Toggle — `true` means LLM calls are active, `false` means use heuristic labels |
| `base-url` | The LLM API base URL (e.g. `https://aipro.sdsdev.co.kr/general/api/v1`) |
| `api-key` | Bearer token for the `Authorization` header |
| `service-id` | Value for the `X-Service-Id` header |
| `user-identifier` | Value for the `user_identifier` header (email) |
| `model-id` | Value for the `model` header (e.g. `"42"`) |
| `max-tokens` | Max tokens the LLM may generate (default: 24576) |
| `temperature` | Controls creativity vs determinism (1.0 = creative) |
| `top-k` | Sampling parameter (0 = disabled) |
| `connect-timeout-ms` | How long to wait when connecting to the LLM (default: 5 seconds) |
| `read-timeout-ms` | How long to wait for the LLM response (default: 60 seconds) |

---

#### `RestClientConfig.java`

**What it does:** Creates and configures the HTTP client that is used to call the LLM API. It sets up connection/read timeouts and the base URL so the calling code only needs to specify the path (e.g. `/chat/completions`).

**In simple terms:** Think of this as setting up a phone call connection. Before you can dial a number, you need a phone (the `RestClient`) configured with the right settings. This class creates that phone and puts it in the Spring container so any service can use it.

**What it configures:**
- Base URL from `LlmProperties.getBaseUrl()`
- Connect timeout from `LlmProperties.getConnectTimeoutMs()`
- Read timeout from `LlmProperties.getReadTimeoutMs()`
- The bean is named `"llmRestClient"` — services inject it using `@Qualifier("llmRestClient")`

---

### 🔧 Service Layer

These classes live in the `service/` package. This is where all the real work happens.

---

#### `SubsystemDiscoveryService.java`

**What it does:** The main orchestrator. It is the "brain" that coordinates all the other services to complete the full pipeline from raw graph to final response.

**In simple terms:** Think of it as a factory manager. It does not do the actual manufacturing itself, but it coordinates all the workers (other services) in the right order: first collect materials, then process, then cluster, then label, then package for delivery.

**Two main methods:**

1. **`discover(request)`** — runs the full pipeline:
   - Collects raw graphs
   - Builds weighted graph
   - Runs Leiden clustering
   - Aggregates clusters into subsystem drafts
   - Labels each subsystem (heuristic or LLM)
   - Assembles and returns the final `SubsystemDiscoveryResponse`

2. **`toLeidenInput(request)`** — stops early after building the weighted graph and returns the raw Leiden input (useful for debugging what the algorithm actually receives)

**Weighting version:** The constant `WEIGHTING_VERSION = "v1-method5-class4-http4-package1"` is included in the response so consumers know exactly what edge weight multipliers were used to produce the result.

---

#### `GraphInputCollector.java`

**What it does:** Decides where to get the graph data from and fetches it.

**In simple terms:** Think of it as a data fetcher that knows two ways to get graph data:
1. **From the request body itself** — if the caller sent inline JSON graphs in the `graphs` field, use those directly
2. **From the database** — if no inline graphs were provided, go to the DB and load them using `applicationId`

**Decision logic:**
- If `request.graphs()` is not empty → use those directly (filtering out any nulls, defaulting missing `graphType` to `CODE_ASSOCIATION`)
- If `request.graphs()` is empty AND `applicationId` is null → throw `IllegalArgumentException` (we have no way to get data)
- If `request.graphs()` is empty AND `applicationId` is provided → call the DB repository
- If the DB returns no graphs → throw `IllegalStateException`

---

#### `WeightedGraphBuilder.java`

**What it does:** Converts raw Analyzer-format graph JSON (lists of nodes and links) into a clean, normalised, weighted graph suitable for the clustering algorithm.

**In simple terms:** Raw graph data is messy — node IDs are just strings, edges have no weights, the same class might appear in multiple graph types. This service cleans all of that up into a consistent internal format.

**7-step transformation process:**

**Step 1 — Read raw nodes and links**
For each `RawGraphDto`, reads `graphType`, `nodeDataArray`, and `linkDataArray`. If `graphType` is missing, defaults to `CODE_ASSOCIATION`.

**Step 2 — Normalise each node**
Every raw node becomes a clean `GraphNode`:
- Converts type to uppercase (`class` → `CLASS`)
- Group nodes (packages) get type `PACKAGE`; regular code nodes default to `CLASS`
- Picks the best display name from `text`, `name`, first field name, or `key`
- Resolves the package name from `packageName` or the parent group chain
- Builds a qualified name like `com.example.order.OrderService`

**Step 3 — Create stable IDs**
Raw keys like `"4"` or `"node-abc"` change between graphs. Instead, the builder creates a **stable key**:
```
TYPE:qualified-name-lowercase
```
For example: `CLASS:com.example.order.orderservice`

This key is then hashed with **SHA-256** and converted to a deterministic `long` number. This means the same logical class always gets the same ID, even across different graph sources.

**Step 4 — Detect relation type**
Each raw link is classified into one of these types:

| Rule | Relation Type |
|---|---|
| Either endpoint is a PACKAGE node | `PACKAGE_RELATION` |
| Either endpoint is a URI, or graph type is `HTTP_API` | `HTTP_FLOW` |
| Graph type is `CALL_GRAPH` or ports (`fromPort`/`toPort`) are present | `METHOD_CALL` |
| Graph type is `CODE_ASSOCIATION` or `CLASS_DIAGRAM` | `CLASS_DEPENDENCY` |
| Nothing matches | `UNKNOWN` |

**Step 5 — Assign weights**
Each relation type gets a weight that reflects how strongly coupled the two nodes are:

| Relation Type | Weight | Why |
|---|---|---|
| `METHOD_CALL` | 5.0 | Direct method calls are the strongest form of coupling |
| `CLASS_DEPENDENCY` | 4.0 | Imports/references are strong but less direct |
| `HTTP_FLOW` | 4.0 | HTTP calls mean tight integration between services |
| `PACKAGE_RELATION` | 1.0 | Package membership is weak structural coupling |
| `UNKNOWN` | 1.0 | Unknown — conservative low weight |

**Step 6 — Merge duplicate edges**
Edges are treated as undirected (`A→B` and `B→A` are the same pair). Multiple edges between the same pair (from different graph types) have their weights summed. Each edge also tracks: total weight, how many times it appeared (`occurrenceCount`), the relation type breakdown, and which graph types it came from.

**Step 7 — Add weak package containment edges**
If a node has a `group` (parent package), a weak edge with weight `1.0` is added between the node and its parent package. This helps isolated classes stay close to their package in clustering, but strong method-call edges still dominate.

---

#### `LeidenClusteringService.java`

**What it does:** The heart of the application. Runs the Leiden community detection algorithm to group nodes into clusters. It runs multiple times (controlled by `runs`) and combines the results using consensus to get stable final clusters.

**In simple terms:** Imagine you have a social network and want to find groups of friends who mostly talk to each other. Leiden is the algorithm that finds those friend groups. Running it multiple times and taking the consensus gives more reliable, stable results.

**How it works — full algorithm breakdown:**

**Phase 1 — Build a local integer graph**
The algorithm needs fast integer-indexed arrays. So it converts the stable `Long` node IDs into dense integers (0, 1, 2, 3…). It stores an adjacency map, weighted degree for every node, total graph weight, and edge count.

**Phase 2 — Local Moving (finding communities)**
1. Each node starts in its own community (N nodes = N communities)
2. Shuffle node order (using a deterministic seed per run so results are reproducible)
3. For each node: temporarily remove it from its community, then look at which neighbouring community would give the best modularity gain
4. The gain formula: `edgeWeightToCommunity − resolution × nodeDegree × communityDegree / (2 × totalWeight)`
5. Move the node to the best community if the gain is positive
6. Repeat until no node wants to move (algorithm has converged)

The `resolution` parameter controls cluster granularity:
- Lower resolution → fewer, larger clusters
- Higher resolution → more, smaller clusters

**Phase 3 — Refinement (fixing disconnected communities)**
After local moving, some communities might contain disconnected sub-groups. For example, nodes A-B and C-D might end up in the same community, but A-B and C-D have no connection to each other. Refinement splits these into separate connected-component clusters, which Louvain (an older algorithm) did not do.

**Phase 4 — Aggregation**
Communities become "super-nodes" in a new, smaller graph. Edges between communities are summed. Local moving runs again on this compressed graph. This repeats for up to `MAX_LEVELS`.

**Phase 5 — Multiple seeded runs**
The `runs` parameter (default 20) controls how many complete clustering attempts run, each with a different random seed. More runs = more stable consensus. Result is N different membership assignments for each node.

**Phase 6 — Consensus clustering**
For small/medium graphs (under 2000 nodes), it compares all N run results:
- Build a co-assignment matrix: how often did each pair of nodes land in the same cluster?
- If the co-assignment fraction ≥ `consensusThreshold`, add an edge between them in a "consensus graph"
- Run one final Leiden pass on the consensus graph to get the definitive clusters

Example with `consensusThreshold=0.7` and `runs=20`:
- Node pair A+B co-clustered in 18 out of 20 runs = 0.90 → they ARE in the same final cluster ✓
- Node pair A+C co-clustered in 6 out of 20 runs = 0.30 → they are NOT in the same cluster ✗

**Phase 7 — Large graph safety**
Consensus requires comparing every pair of nodes, which becomes expensive for large graphs. Safety limits:
- `MAX_CONSENSUS_NODES = 2000`
- `MAX_CONSENSUS_PAIRS = 2,000,000`

If exceeded, the service skips consensus and uses the best single-run result directly.

**Output:** A `LeidenClusteringResult` containing:
- `clusters`: a map of `nodeId → clusterName` (e.g. `1001 → "cluster_3"`)
- `stabilityScores`: a map of `clusterName → stability` (how consistently the cluster appeared across runs)

---

#### `LeidenClusteringResult.java`

**What it does:** A simple data container (model class) that holds the output of the Leiden algorithm.

**In simple terms:** Just a bag that carries two pieces of information out of the clustering algorithm.

**Contains:**
- `clusters: Map<Long, String>` — for every node ID, which cluster does it belong to? (e.g. `{1001: "cluster_3", 1002: "cluster_3", 1003: "cluster_7"}`)
- `stabilityScores: Map<String, Double>` — for every cluster, how stable was it across runs? A score of `1.0` means it appeared identically in every run.

---

#### `ClusterService.java`

**What it does:** Takes the raw clustering output (just a map of node→cluster) and enriches it into full, human-readable subsystem summaries. It also detects cross-subsystem links.

**In simple terms:** After clustering, you know which nodes belong to which cluster. But that is just a list of numbers. `ClusterService` enriches each cluster with meaningful information: How many nodes? Which Java packages dominate? Which nodes are the most important (central)? Which REST endpoints are in this cluster? How strongly is this cluster coupled to others?

**What it calculates for each cluster:**

**Central Nodes** — "Who are the most important classes in this subsystem?"
- For each non-PACKAGE node: calculate its internal weighted degree (sum of edge weights to other nodes *in the same cluster*)
- Sort by score descending, take top 10
- Score is normalised to 0.0–1.0 (the most connected node gets 1.0)
- Package nodes are excluded from this list (they are structural, not semantic)

**Top Packages** — "Which Java packages are most represented?"
- Count how many nodes in the cluster have each package name
- Sort by count descending, take top 5
- Gives you `["com.example.user", "com.example.auth"]`

**API Endpoints** — "Which REST endpoints live in this subsystem?"
- Finds all nodes with type `URI`
- Parses their qualified names to extract method + path:
  - `GET /api/users` or `/api/users:GET` → `{method: "GET", path: "/api/users"}`
- Returns up to 20 endpoints

**Internal Connectivity** — "How densely connected is this subsystem internally?"
- Formula: `internalEdgeCount / ((nodeCount × (nodeCount - 1)) / 2)`
- 1.0 = every node connects to every other node (extremely cohesive)
- 0.0 = no internal edges (all connections go cross-boundary)

**Cross-Subsystem Links** — "Which other subsystems does this one talk to?"
- For every edge where source and target are in *different* clusters, that edge becomes a `SubsystemLinkDto`
- Links accumulate weight, edge count, and relation type breakdown
- Coupling strength is then classified:
  - `HIGH` → weight ≥ 20 or edge count ≥ 10
  - `MEDIUM` → weight ≥ 8 or edge count ≥ 4
  - `LOW` → everything else

**Subsystems are sorted** by node count descending — the largest subsystem comes first.

---

#### `ClusterAggregation.java`

**What it does:** A simple container that holds all the outputs produced by `ClusterService` in one place.

**In simple terms:** It is a bag with three compartments:
1. `subsystemDrafts` — the list of enriched-but-not-yet-labelled subsystems
2. `subsystemLinks` — the cross-subsystem coupling edges
3. `nodeAssignments` — the flat mapping of every node ID to its subsystem ID

The word "Draft" in `subsystemDrafts` means these subsystems have all their metrics and structure, but they do not yet have a human-readable name — that comes from `SubsystemLabelService` in the next step.

---

#### `SubsystemDraft.java`

**What it does:** A data holder (model class) representing a subsystem *before* it has been given a name and description by the label service.

**In simple terms:** Think of it as a "work in progress" subsystem. It has all the structural data (nodes, edges, packages, API endpoints, metrics) but still needs a name.

**Fields:**
| Field | Type | What It Holds |
|---|---|---|
| `id` | `String` | Internal cluster name like `"cluster_3"` |
| `stabilityScore` | `double` | How consistently this cluster appeared across runs (0–1) |
| `nodeCount` | `int` | Total nodes in this cluster |
| `edgeCount` | `int` | Total internal edges |
| `internalConnectivity` | `double` | Edge density within the cluster |
| `topPackages` | `List<String>` | Most common Java packages |
| `centralNodes` | `List<CentralNodeDto>` | Top 10 most-connected nodes |
| `apiEndpoints` | `List<ApiEndpointDto>` | REST endpoints found in this cluster |
| `nodes` | `List<NodeSummaryDto>` | All nodes in the cluster |

---

#### `SubsystemLabelService.java`

**What it does:** Gives each subsystem a human-readable name and description. It can do this in two ways: using an AI (LLM), or using a built-in heuristic algorithm.

**In simple terms:** After clustering, each group has an ID like `"cluster_3"`. This service replaces that with something meaningful like `"User Management"` along with a description like `"Handles authentication and user profile management"`.

**Two modes:**

**Mode 1 — LLM Labelling** (when `useLlmLabels=true` AND `llm.enabled=true`):

1. Build a text prompt describing the cluster:
   ```
   You are labeling a software subsystem...
   Cluster ID: cluster_3
   Top nodes:
   - UserService [class]
   - AuthController [class]
   Top packages:
   - com.example.user
   API endpoints:
   - POST /api/users/register
   ```
2. Call the LLM API via `callLlm()`:
   - `POST {base-url}/chat/completions`
   - Headers: `Authorization: Bearer {apiKey}`, `X-Service-Id: {serviceId}`, `user_identifier: {userIdentifier}`, `model: {modelId}`, `Content-Type: application/json`
   - Body: `{ "messages": [{"role":"user","content":"..."}], "max_tokens": 24576, "temperature": 1, "top_k": 0 }`
3. Read `choices[0].message.content` from the response
4. Parse it as JSON to get `name`, `description`, `confidence`
5. If parsing fails → fall through to heuristic mode

**Mode 2 — Heuristic Labelling** (fallback):

Generates a name from the cluster's content using simple rules:
1. Split CamelCase names of central nodes into words (e.g. `"UserService"` → `["user"]`)
2. Filter out common technical suffixes (`"service"`, `"controller"`, `"repository"`, etc.)
3. Take the first meaningful word and capitalise it → primary word (e.g. `"User"`)
4. Detect a secondary word based on node types:
   - Has a `@Controller` or API endpoints → secondary = `"Operations"`
   - Has `Mapper` or `Repository` → secondary = `"Data"`
   - Otherwise → secondary = `"Management"`
5. Final name: `"User Operations"` or `"User Data"` or `"User Management"`
6. `llmConfidence` is set to `0.5` for heuristic labels (vs the LLM's own confidence score)

**Fallback triggers:**
- `useLlmLabels=false` in the request
- `llm.enabled=false` in config
- `api-key` is empty
- Any exception during the LLM call (network error, timeout, bad response, etc.)

The fallback is **silent** — the caller never sees an error, just gets heuristic names.

---

#### `LabelResult.java`

**What it does:** A simple record (immutable data class) that holds the result of labelling a subsystem.

**Fields:**
- `name` — the subsystem name (e.g. `"User Management"`)
- `description` — short description (max ~25 words)
- `confidence` — how confident we are in the label (0.0–1.0)

---

#### `DemoGraphCatalog.java`

**What it does:** Provides hardcoded example graphs for demonstration and testing purposes, so users can try the application without needing a real database.

**In simple terms:** This is the demo data library. It contains several pre-built graph examples (like an e-commerce system, a real estate platform, etc.) and also has a method to generate random synthetic graphs of any size.

**What it provides:**
- `listExamples()` — returns a list of all available demo graphs with their names and descriptions
- `getExampleGraphs(key)` — returns the actual raw graph data for a named example
- `buildSyntheticGraphs(nodeCount)` — generates a random Analyzer-style call graph with `nodeCount` nodes, useful for performance testing

---

### 📐 Domain Model

These classes live in the `domain/` package. They represent the internal data structures used by the algorithm — not the input/output format, but the working data model inside the application.

---

#### `GraphNode.java`

**What it does:** Represents a single node in the internal weighted graph. This is the normalised, clean version of a raw node from the input JSON.

**In simple terms:** While a raw node from input might look like `{ "key": "4", "text": "UserService", "type": "CLASS" }`, a `GraphNode` is the cleaned-up, enriched version with a stable numeric ID, qualified name, package name, and other metadata.

**Fields:**
| Field | Type | Meaning |
|---|---|---|
| `id` | `Long` | Stable numeric ID (SHA-256 hash of the stable key) |
| `stableKey` | `String` | Composite key like `"CLASS:com.example.userservice"` |
| `name` | `String` | Simple display name like `"UserService"` |
| `qualifiedName` | `String` | Fully qualified name like `"com.example.user.UserService"` |
| `type` | `String` | Node type: `"CLASS"`, `"PACKAGE"`, `"URI"`, etc. |
| `packageName` | `String` | Java package name like `"com.example.user"` |
| `sourceGraphs` | `Set<GraphType>` | Which graph types this node appeared in |
| `metadata` | `Map<String, Object>` | Extra data (e.g. field names) |

---

#### `WeightedEdge.java`

**What it does:** Represents a single edge in the internal weighted graph. An edge connects two nodes and carries information about the strength and nature of their relationship.

**In simple terms:** It is the connection between two `GraphNode`s. When `UserService` calls `UserRepository`, that becomes a `WeightedEdge` with source=UserService, target=UserRepository, weight=5.0 (METHOD_CALL), occurrenceCount=1.

**Fields:**
| Field | Type | Meaning |
|---|---|---|
| `source` | `Long` | ID of the source node |
| `target` | `Long` | ID of the target node |
| `weight` | `double` | Coupling strength (accumulated from all occurrences) |
| `occurrenceCount` | `int` | How many raw links contributed to this edge |
| `relationTypes` | `Map<RelationType, Integer>` | Breakdown by relation type |
| `sourceGraphs` | `Set<GraphType>` | Which graph types this edge appeared in |

**Key method — `addOccurrence(weight, relationType, graphType)`:** Merges another occurrence of the same pair into this edge — adds to weight, increments count, tracks relation type.

---

#### `WeightedGraph.java`

**What it does:** The complete internal graph — a container holding all `GraphNode`s and all `WeightedEdge`s together.

**In simple terms:** It is the full picture — all the nodes and all their connections — ready to be fed into the clustering algorithm.

**Contains:**
- `List<GraphNode> nodes`
- `List<WeightedEdge> edges`

---

#### `GraphType.java` (enum)

**What it does:** An enumeration of the four types of dependency graphs the application understands.

| Value | Meaning |
|---|---|
| `CODE_ASSOCIATION` | Static import/reference relationships between classes |
| `CALL_GRAPH` | Runtime method call relationships |
| `HTTP_API` | HTTP-level calls between services |
| `CLASS_DIAGRAM` | UML inheritance / composition relationships |

---

#### `RelationType.java` (enum)

**What it does:** An enumeration of the five types of relationships an edge can represent.

| Value | Meaning |
|---|---|
| `METHOD_CALL` | One method calls another method |
| `CLASS_DEPENDENCY` | One class depends on another (import/field/parameter) |
| `HTTP_FLOW` | One service makes an HTTP call to another |
| `PACKAGE_RELATION` | A class is contained in a package |
| `UNKNOWN` | Could not be classified |

---

### 📋 Data Transfer Objects (DTOs)

These classes live in the `dto/` package. They are the shapes of data that flow in and out of the REST API — the request and response JSON models.

---

#### `DiscoverSubsystemRequest.java`

**What it does:** The shape of the JSON body that clients send to the discovery endpoint.

**Fields:**
| Field | Type | Default | Description |
|---|---|---|---|
| `applicationId` | `Long` | — | DB app ID (required when no inline graphs) |
| `applicationKey` | `String` | — | Human-readable app name (used in LLM prompts) |
| `graphTypes` | `List<GraphType>` | all | Filter to specific graph types |
| `runs` | `Integer` | 20 | How many Leiden runs to perform |
| `consensusThreshold` | `Double` | 0.7 | Min co-cluster fraction to merge node pairs |
| `resolution` | `Double` | 1.0 | Algorithm granularity |
| `useLlmLabels` | `Boolean` | false | Whether to call the LLM for names |
| `graphs` | `List<RawGraphDto>` | — | Inline graph data (if not using DB) |

Helper methods: `runsOrDefault()`, `consensusThresholdOrDefault()`, `resolutionOrDefault()`, `useLlmLabelsOrDefault()` — each returns the field value or the default if null.

---

#### `SubsystemDiscoveryResponse.java`

**What it does:** The shape of the JSON returned by the discovery endpoint.

**Fields:**
- `applicationId` — echoed back from the request
- `applicationKey` — echoed back from the request
- `algorithm` — an `AlgorithmInfoDto` describing the algorithm settings used
- `summary` — a `SummaryDto` with overall graph statistics
- `subsystems` — the discovered subsystems (list of `SubsystemDto`)
- `subsystemLinks` — coupling between subsystems (list of `SubsystemLinkDto`)
- `nodeAssignments` — every node mapped to its subsystem (list of `NodeAssignmentDto`)

---

#### `AlgorithmInfoDto.java`

**What it does:** Records exactly which algorithm settings were used for a run, so the response is reproducible and auditable.

**Fields:** `name`, `runs`, `consensusThreshold`, `resolution`, `weightingVersion`

---

#### `SummaryDto.java`

**What it does:** High-level statistics about the graph and the results.

**Fields:** `totalNodes`, `totalEdges`, `subsystemCount`, `averageStability`

---

#### `SubsystemDto.java`

**What it does:** Represents one discovered subsystem in the response — the fully enriched, labelled version.

**Fields:**
| Field | Type | Description |
|---|---|---|
| `id` | `String` | Internal ID like `"cluster_3"` |
| `name` | `String` | Human-readable name like `"User Management"` |
| `description` | `String` | Short description (from heuristic or LLM) |
| `stabilityScore` | `double` | How stable this cluster was (0–1) |
| `llmConfidence` | `double` | LLM's confidence in the label (0.5 if heuristic) |
| `nodeCount` | `int` | Number of nodes in this subsystem |
| `edgeCount` | `int` | Number of internal edges |
| `internalConnectivity` | `double` | Edge density (0–1) |
| `topPackages` | `List<String>` | Most represented Java packages |
| `centralNodes` | `List<CentralNodeDto>` | Top hub nodes by internal degree |
| `apiEndpoints` | `List<ApiEndpointDto>` | REST endpoints in this subsystem |
| `nodes` | `List<NodeSummaryDto>` | All nodes in the subsystem |

---

#### `SubsystemLinkDto.java`

**What it does:** Represents a coupling relationship between two subsystems.

**Fields:** `source` (cluster ID), `target` (cluster ID), `weight`, `edgeCount`, `relationTypes` (breakdown map), `couplingStrength` (`"LOW"` / `"MEDIUM"` / `"HIGH"`)

---

#### `NodeAssignmentDto.java`

**What it does:** Maps a single node to its subsystem. Used to build a complete flat assignment table.

**Fields:** `nodeId` (the stable Long ID), `subsystemId` (the cluster name), `membershipScore` (always 1.0 currently)

---

#### `RawGraphDto.java`

**What it does:** Represents one graph in the input format — exactly as sent by the caller (or loaded from DB).

**Fields:** `graphType`, `nodeDataArray` (list of `RawNodeDto`), `linkDataArray` (list of `RawLinkDto`)

---

#### `RawNodeDto.java`

**What it does:** Represents one node in the raw input graph (before any normalisation).

**Fields:** `key`, `text`, `name`, `type`, `packageName`, `group`, `isGroup`, `fields` (list of `RawFieldDto`)

---

#### `RawLinkDto.java`

**What it does:** Represents one edge in the raw input graph.

**Fields:** `from`, `to`, `fromPort`, `toPort`, `type`

---

#### `RawFieldDto.java`

**What it does:** Represents a field or method attached to a raw node.

**Fields:** `key`, `name`, `type`, `colorFlag`

---

#### `CentralNodeDto.java`

**What it does:** Represents one high-centrality node in a subsystem — a hub class.

**Fields:** `id`, `name`, `qualifiedName`, `type`, `score` (0.0–1.0 centrality score)

---

#### `NodeSummaryDto.java`

**What it does:** A lightweight summary of a node, used in the `nodes` list of each subsystem.

**Fields:** `id`, `name`, `qualifiedName`, `type`, `packageName`

---

#### `ApiEndpointDto.java`

**What it does:** Represents one REST API endpoint found within a subsystem.

**Fields:** `id` (node ID), `method` (`GET`, `POST`, etc.), `path` (URL path)

---

#### `LeidenInputDto.java`

**What it does:** The format returned by the `leiden-input` endpoints — shows exactly what the algorithm receives after all raw graph transformation is done.

**Fields:** `nodeCount`, `edgeCount`, `nodes` (list of node IDs), `edges` (list of `LeidenInputEdgeDto`)

---

#### `LeidenInputEdgeDto.java`

**What it does:** One edge in the Leiden input format.

**Fields:** `source` (Long ID), `target` (Long ID), `weight`

---

#### `ExampleGraphSummaryDto.java`

**What it does:** Summary info about one demo graph available in the catalog.

**Fields:** `key`, `name`, `description`, `estimatedNodeCount`, `estimatedEdgeCount`, `graphTypes`, `synthetic`

---

### 🗄️ Repository Layer

These classes live in the `repository/` package. They handle loading data from the database.

---

#### `GraphInputRepository.java` (interface)

**What it does:** Defines the contract for loading raw graph data from the database. Any implementation must provide a `loadGraphs(applicationId, graphTypes)` method.

**In simple terms:** An interface is like a blueprint or a contract. Any class that implements this interface promises to provide a method that, given an application ID and a list of graph types, returns the matching raw graphs.

---

#### `TbNodeRelationGraphInputRepository.java`

**What it does:** The actual database implementation of `GraphInputRepository`. It reads data from three database tables (`tb_node`, `tb_node_detail`, `tb_node_relation`) and converts the rows into `RawGraphDto` objects.

**In simple terms:** This class knows how to talk to the database. It uses **MyBatis** (a SQL framework) to run SQL queries defined in XML files, fetch the raw rows, and convert them into the format the rest of the application expects.

**Tables it reads:**
- `tb_node` — nodes (classes, packages, URIs)
- `tb_node_detail` — additional node metadata (method fields, etc.)
- `tb_node_relation` — edges/links between nodes

**For PostgreSQL (production):** Configure the datasource in `application-postgres-example.yml`.
**For H2 (local dev):** The `schema.sql` file creates demo versions of these tables automatically when the app starts.

---

## 6. Configuration Reference

Main config file: `java/src/main/resources/application.yml`

```yaml
server:
  port: 8081
  servlet:
    context-path: /codeanalyzer/server

spring:
  datasource:
    # Local H2 in-memory DB (default for development)
    url: jdbc:h2:mem:subsystem_discovery;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
    username: sa
    password:
    driver-class-name: org.h2.Driver
  sql:
    init:
      mode: always   # Runs schema.sql on startup

subsystem:
  llm:
    enabled: false                                        # Set true to activate LLM
    base-url: https://aipro.sdsdev.co.kr/general/api/v1  # LLM gateway URL
    api-key: 5a502d87-efb4-4783-8e2e-521331ae6e01         # Bearer token
    service-id: Analyzer                                  # X-Service-Id header
    user-identifier: s.mayank@samsung.com                 # user_identifier header
    model-id: "42"                                        # model header
    max-tokens: 24576
    temperature: 1.0
    top-k: 0
    connect-timeout-ms: 5000
    read-timeout-ms: 60000
```

**To switch to PostgreSQL** (production), replace the datasource block with your real connection details and set `sql.init.mode: never`.

---

## 7. REST API Reference

All URLs are relative to the base: `http://localhost:8081/codeanalyzer/server`

### Primary Endpoints

| Method | URL | Description |
|---|---|---|
| `POST` | `/api/codeanalyzer/analysis/{id}/subsystem/discover` | **Main endpoint** — DB-backed discovery |
| `POST` | `/api/codeanalyzer/analysis/{id}/subsystem/leiden-input` | Preview Leiden input for a DB app |
| `POST` | `/api/codeanalyzer/subsystems/transform/leiden-input` | Transform inline JSON to Leiden input |

### Legacy (backward-compatible) Endpoints

| Method | URL | Description |
|---|---|---|
| `POST` | `/api/analysis/{id}/subsystems/discover` | Same as primary discover |
| `POST` | `/api/subsystems/discover` | Inline-graph discovery (no DB) |
| `POST` | `/api/subsystems/transform/leiden-input` | Same as codeanalyzer transform |

### Demo Endpoints

| Method | URL | Description |
|---|---|---|
| `GET` | `/api/subsystems/examples` | List all demo graphs |
| `GET` | `/api/subsystems/examples/{key}/raw-graph` | Raw graph data for a demo |
| `GET` | `/api/subsystems/examples/{key}/leiden-input` | Leiden input for a demo |
| `POST` | `/api/subsystems/examples/{key}/discover` | Run discovery on a demo |
| `GET` | `/api/subsystems/generated/{nodeCount}/raw-graph` | Synthetic random graph |
| `GET` | `/api/subsystems/generated/{nodeCount}/leiden-input` | Leiden input for synthetic graph |
| `POST` | `/api/subsystems/generated/{nodeCount}/discover` | Discover on synthetic graph |

### Query Parameters for discover endpoints

| Parameter | Type | Default | Description |
|---|---|---|---|
| `runs` | `int` | 20 | Number of algorithm runs |
| `consensusThreshold` | `double` | 0.7 | Merge threshold |
| `resolution` | `double` | 1.0 | Cluster granularity |
| `useLlmLabels` | `boolean` | `false` | Enable LLM naming |

---

## 8. Input & Output Formats

### Minimal request body

```json
{
  "applicationKey": "my-app",
  "runs": 20,
  "consensusThreshold": 0.7,
  "resolution": 1.0,
  "useLlmLabels": false
}
```

### Inline graph request body (standalone mode)

```json
{
  "applicationId": 1,
  "applicationKey": "demo",
  "graphs": [
    {
      "graphType": "CALL_GRAPH",
      "nodeDataArray": [
        { "key": "1", "text": "UserService", "type": "CLASS", "group": "2" },
        { "key": "2", "text": "com.example.user", "type": "PACKAGE", "isGroup": true },
        { "key": "3", "text": "UserRepository", "type": "CLASS", "group": "2" }
      ],
      "linkDataArray": [
        { "from": "1", "to": "3", "fromPort": "findById", "toPort": "findById" }
      ]
    }
  ]
}
```

### Discovery response shape

```json
{
  "applicationId": 39956,
  "applicationKey": "analyzer-static-agent",
  "algorithm": {
    "name": "Leiden (Java)",
    "runs": 20,
    "consensusThreshold": 0.7,
    "resolution": 1.0,
    "weightingVersion": "v1-method5-class4-http4-package1"
  },
  "summary": {
    "totalNodes": 350,
    "totalEdges": 1420,
    "subsystemCount": 16,
    "averageStability": 0.83
  },
  "subsystems": [
    {
      "id": "cluster_1",
      "name": "User Management",
      "description": "Handles user registration, authentication and profiles.",
      "stabilityScore": 0.91,
      "llmConfidence": 0.87,
      "nodeCount": 24,
      "edgeCount": 112,
      "internalConnectivity": 0.078,
      "topPackages": ["com.example.user", "com.example.auth"],
      "centralNodes": [
        { "id": 1001, "name": "UserService", "qualifiedName": "com.example.user.UserService", "type": "CLASS", "score": 1.0 }
      ],
      "apiEndpoints": [
        { "id": 2001, "method": "POST", "path": "/api/users/register" }
      ],
      "nodes": [ ... ]
    }
  ],
  "subsystemLinks": [
    {
      "source": "cluster_1",
      "target": "cluster_2",
      "weight": 4.7,
      "edgeCount": 12,
      "relationTypes": { "METHOD_CALL": 8, "CLASS_DEPENDENCY": 4 },
      "couplingStrength": "MEDIUM"
    }
  ],
  "nodeAssignments": [
    { "nodeId": 1001, "subsystemId": "cluster_1", "membershipScore": 1.0 }
  ]
}
```

### Error response shape

```json
{
  "timestamp": "2026-06-01T05:30:00Z",
  "status": 400,
  "error": "IllegalArgumentException",
  "message": "applicationId is required when inline graph JSON is not provided"
}
```

---

## 9. Interactive Architecture Visualizer

For a visual, step-by-step animated walkthrough of the full backend pipeline, open this file in your browser:

```
subsystem-architecture-visualizer.html
```

It covers all 7 stages — API entry, graph loading, weight normalisation, Leiden algorithm, subsystem assembly, LLM labelling, and the final response — with code examples, real data, and interactive navigation.

Use keyboard arrows `←` / `→` or the **▶ Auto Play** button to step through the animation.

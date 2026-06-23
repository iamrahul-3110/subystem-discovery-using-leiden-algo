package com.example.subsystemdiscovery.dataset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Service
public class SyntheticDataGenerator {

    private static final Logger log = LoggerFactory.getLogger(SyntheticDataGenerator.class);
    private static final DateTimeFormatter SNAPSHOT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final int BATCH_SIZE = 2_000;

    private final JdbcTemplate jdbcTemplate;

    public SyntheticDataGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public GeneratedDataset generateDataset(DomainTemplate template, int nodeCount) {
        validateNodeCount(nodeCount);
        log.info("Starting synthetic dataset generation for template={} with nodeCount={}", template, nodeCount);

        long applicationId = 1_000L + template.ordinal() + 1;
        String applicationKey = template.name().toLowerCase(Locale.ROOT) + "-graph-intelligence";
        String applicationName = displayName(template) + " Graph Intelligence POC";
        String analysisTime = LocalDateTime.now().format(SNAPSHOT_FORMAT);
        Random random = new Random(42L + template.ordinal() * 10_000L + nodeCount);

        upsertApplication(applicationId, applicationName, applicationKey);
        upsertSnapshot(applicationId, analysisTime);

        List<String> domains = domainsFor(template);
        List<DomainRange> ranges = generateNodes(applicationId, analysisTime, template, domains, nodeCount);
        int relationCount = generateRelations(applicationId, analysisTime, ranges, random);

        log.info("Successfully generated synthetic dataset: appId={}, nodeCount={}, relationCount={}",
                applicationId, nodeCount, relationCount);

        return new GeneratedDataset(
                applicationId,
                applicationKey,
                applicationName,
                analysisTime,
                nodeCount,
                relationCount,
                template.name()
        );
    }

    private void validateNodeCount(int nodeCount) {
        if (nodeCount != 100 && nodeCount != 1_000 && nodeCount != 10_000 && nodeCount != 50_000) {
            throw new IllegalArgumentException("nodeCount must be one of 100, 1000, 10000, 50000");
        }
    }

    private void upsertApplication(long applicationId, String applicationName, String applicationKey) {
        log.info("Upserting application master: ID={}, Key={}, Name={}", applicationId, applicationKey, applicationName);
        jdbcTemplate.update("""
                MERGE INTO tb_application_master (application_id, application_name, application_key)
                KEY(application_id)
                VALUES (?, ?, ?)
                """, applicationId, applicationName, applicationKey);
    }

    private void upsertSnapshot(long applicationId, String analysisTime) {
        log.info("Upserting snapshot: ID={}, analysisTime={}", applicationId, analysisTime);
        jdbcTemplate.update("""
                MERGE INTO tb_node_history_master (application_id, analysis_time, created_at)
                KEY(application_id, analysis_time)
                VALUES (?, CAST(? AS TIMESTAMP), CURRENT_TIMESTAMP)
                """, applicationId, analysisTime);
    }

    private List<DomainRange> generateNodes(long applicationId,
                                            String analysisTime,
                                            DomainTemplate template,
                                            List<String> domains,
                                            int nodeCount) {
        log.info("Generating {} nodes across {} business domains...", nodeCount, domains.size());
        String nodeSql = """
                INSERT INTO tb_node_history (
                    application_id, analysis_time, node_id, node_name, node_type, use_yn
                ) VALUES (?, CAST(? AS TIMESTAMP), ?, ?, ?, TRUE)
                """;
        String detailSql = """
                INSERT INTO tb_node_detail_history (
                    application_id, analysis_time, node_id, split_node_level, split_node_name, split_node_type
                ) VALUES (?, CAST(? AS TIMESTAMP), ?, ?, ?, ?)
                """;

        List<Object[]> nodeBatch = new ArrayList<>(BATCH_SIZE);
        List<Object[]> detailBatch = new ArrayList<>(BATCH_SIZE * 3);
        List<DomainRange> ranges = new ArrayList<>();

        int baseCount = nodeCount / domains.size();
        int remainder = nodeCount % domains.size();
        long nextNodeId = 1L;

        for (int domainIndex = 0; domainIndex < domains.size(); domainIndex++) {
            String domain = domains.get(domainIndex);
            int domainNodeCount = baseCount + (domainIndex < remainder ? 1 : 0);
            long start = nextNodeId;
            long end = start + domainNodeCount - 1;
            DomainRange range = new DomainRange(domain, toSlug(domain), start, end);
            ranges.add(range);

            for (long nodeId = start; nodeId <= end; nodeId++) {
                int localIndex = (int) (nodeId - start);
                String packageName = packageName(template, range.slug(), localIndex);
                String className = toPascal(domain) + componentSuffix(localIndex) + localIndex;
                String methodName = methodPrefix(localIndex) + toPascal(domain) + "Flow" + localIndex;
                String nodeName = packageName + "." + className + "." + methodName + "()";

                nodeBatch.add(new Object[]{
                        applicationId, analysisTime, nodeId, nodeName, "METHOD"
                });
                detailBatch.add(new Object[]{
                        applicationId, analysisTime, nodeId, 0, methodName + "()", "METHOD"
                });
                detailBatch.add(new Object[]{
                        applicationId, analysisTime, nodeId, 1, className, "CLASS"
                });
                detailBatch.add(new Object[]{
                        applicationId, analysisTime, nodeId, 2, packageName, "PACKAGE"
                });

                if (nodeBatch.size() >= BATCH_SIZE) {
                    log.debug("Flushing node/detail batch (size={})", nodeBatch.size());
                    flush(nodeSql, nodeBatch);
                    flush(detailSql, detailBatch);
                }
            }
            nextNodeId = end + 1;
        }

        if (!nodeBatch.isEmpty()) {
            log.debug("Flushing final node/detail batch (size={})", nodeBatch.size());
        }
        flush(nodeSql, nodeBatch);
        flush(detailSql, detailBatch);
        log.info("Node and detail records successfully generated and saved.");
        return ranges;
    }

    private int generateRelations(long applicationId,
                                  String analysisTime,
                                  List<DomainRange> ranges,
                                  Random random) {
        log.info("Generating relations between domains...");
        String relationSql = """
                INSERT INTO tb_node_relation_history (
                    application_id, analysis_time, relation_id, source_node_id, target_node_id, relation_type
                ) VALUES (?, CAST(? AS TIMESTAMP), ?, ?, ?, ?)
                """;

        List<Object[]> relationBatch = new ArrayList<>(BATCH_SIZE);
        long relationId = 1L;

        for (int rangeIndex = 0; rangeIndex < ranges.size(); rangeIndex++) {
            DomainRange range = ranges.get(rangeIndex);
            for (long source = range.start(); source <= range.end(); source++) {
                int internalEdges = 3 + random.nextInt(5);
                for (int i = 0; i < internalEdges; i++) {
                    long target = randomNode(range, random);
                    if (target != source) {
                        relationBatch.add(relationRow(applicationId, analysisTime, relationId++, source, target));
                    }
                }

                if (random.nextDouble() < 0.22d) {
                    DomainRange externalRange = ranges.get((rangeIndex + 1 + random.nextInt(ranges.size() - 1)) % ranges.size());
                    long target = randomNode(externalRange, random);
                    relationBatch.add(relationRow(applicationId, analysisTime, relationId++, source, target));
                }

                if (relationBatch.size() >= BATCH_SIZE) {
                    log.debug("Flushing relation batch (size={})", relationBatch.size());
                    flush(relationSql, relationBatch);
                }
            }
        }

        if (!relationBatch.isEmpty()) {
            log.debug("Flushing final relation batch (size={})", relationBatch.size());
        }
        flush(relationSql, relationBatch);
        log.info("Relations successfully generated. Total relations: {}", relationId - 1);
        return Math.toIntExact(relationId - 1);
    }

    private Object[] relationRow(long applicationId,
                                 String analysisTime,
                                 long relationId,
                                 long source,
                                 long target) {
        return new Object[]{
                applicationId, analysisTime, relationId, source, target, "METHOD_CALL"
        };
    }

    private void flush(String sql, List<Object[]> batch) {
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
            batch.clear();
        }
    }

    private long randomNode(DomainRange range, Random random) {
        return range.start() + random.nextInt(Math.toIntExact(range.end() - range.start() + 1));
    }

    private String packageName(DomainTemplate template, String domainSlug, int index) {
        String layer = switch (index % 5) {
            case 0 -> "api";
            case 1 -> "service";
            case 2 -> "workflow";
            case 3 -> "data";
            default -> "integration";
        };
        return "com.graphintelligence." + template.name().toLowerCase(Locale.ROOT) + "." + domainSlug + "." + layer;
    }

    private String componentSuffix(int index) {
        return switch (index % 6) {
            case 0 -> "Controller";
            case 1 -> "Service";
            case 2 -> "Workflow";
            case 3 -> "Repository";
            case 4 -> "Client";
            default -> "Policy";
        };
    }

    private String methodPrefix(int index) {
        return switch (index % 7) {
            case 0 -> "orchestrate";
            case 1 -> "validate";
            case 2 -> "resolve";
            case 3 -> "persist";
            case 4 -> "publish";
            case 5 -> "hydrate";
            default -> "calculate";
        };
    }

    private List<String> domainsFor(DomainTemplate template) {
        return switch (template) {
            case AMAZON -> List.of(
                    "Customer Identity", "Product Catalog", "Search Ranking", "Recommendation Engine",
                    "Shopping Cart", "Checkout", "Payments", "Order Fulfillment",
                    "Inventory", "Shipping", "Reviews", "Notifications"
            );
            case SWIGGY -> List.of(
                    "Customer Identity", "Restaurant Onboarding", "Menu Catalog", "Cart",
                    "Order Placement", "Delivery Dispatch", "Payments", "Partner Operations",
                    "Live Tracking", "Promotions", "Ratings", "Notifications"
            );
            case BLINKIT -> List.of(
                    "Customer Identity", "Dark Store Inventory", "Product Catalog", "Cart",
                    "Checkout", "Payments", "Picker Assignment", "Rider Dispatch",
                    "Delivery Tracking", "Substitution", "Promotions", "Notifications"
            );
            case ZEPTO -> List.of(
                    "Customer Identity", "Micro Warehouse", "Product Catalog", "Cart",
                    "Checkout", "Payments", "Batch Picking", "Rider Allocation",
                    "ETA Prediction", "Inventory Replenishment", "Offers", "Notifications"
            );
            case MYNTRA -> List.of(
                    "Customer Identity", "Fashion Catalog", "Search Discovery", "Personalization",
                    "Wishlist", "Cart", "Payments", "Order Management",
                    "Returns", "Seller Operations", "Reviews", "Notifications"
            );
            case MAKEMYTRIP -> List.of(
                    "Customer Identity", "Flight Search", "Hotel Search", "Itinerary Planning",
                    "Booking", "Payments", "Refunds", "Offers",
                    "Reviews", "Loyalty", "Travel Alerts", "Notifications"
            );
        };
    }

    private String displayName(DomainTemplate template) {
        return switch (template) {
            case AMAZON -> "Amazon";
            case SWIGGY -> "Swiggy";
            case BLINKIT -> "Blinkit";
            case ZEPTO -> "Zepto";
            case MYNTRA -> "Myntra";
            case MAKEMYTRIP -> "MakeMyTrip";
        };
    }

    private String toSlug(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private String toPascal(String value) {
        StringBuilder result = new StringBuilder();
        for (String part : value.split("[^A-Za-z0-9]+")) {
            if (!part.isBlank()) {
                result.append(part.substring(0, 1).toUpperCase(Locale.ROOT))
                        .append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return result.isEmpty() ? "Domain" : result.toString();
    }

    private record DomainRange(String name, String slug, long start, long end) {
    }

    public record GeneratedDataset(
            Long applicationId,
            String applicationKey,
            String applicationName,
            String analysisTime,
            int nodeCount,
            int relationCount,
            String template
    ) {
    }
}

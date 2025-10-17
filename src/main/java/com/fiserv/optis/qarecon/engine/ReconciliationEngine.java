package com.fiserv.optis.qarecon.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fiserv.optis.qarecon.model.*;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.client.*;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ReconciliationEngine {

    private final MongoClient source;
    private final MongoClient target;

    public ReconciliationEngine(MongoClient source, MongoClient target) {
        this.source = source;
        this.target = target;
    }

    public ReconciliationResult run(ReconciliationSpec spec) {
        String runId = spec.runId != null ? spec.runId : String.valueOf(System.currentTimeMillis());
        spec.runId = runId;

        // Fetch
        List<Document> sourceDocs = fetch(source, spec, Side.SOURCE);
        List<Document> rightDocs = fetch(target, spec, Side.TARGET);

        // Key by logical key
        Map<String, Document> sourceMap = toMap(sourceDocs, spec, Side.SOURCE);
        Map<String, Document> targetMap = toMap(rightDocs, spec, Side.TARGET);

        // Partitions
        Set<String> sourceKeys = sourceMap.keySet();
        Set<String> targetKeys = targetMap.keySet();

        Set<String> matched = new HashSet<>(sourceKeys);
        matched.retainAll(targetKeys);
        Set<String> sourceOnly = new HashSet<>(sourceKeys);
        sourceOnly.removeAll(targetKeys);
        Set<String> targetOnly = new HashSet<>(targetKeys);
        targetOnly.removeAll(sourceKeys);

        // Totals
        Map<String, BigDecimal> leftTotals = totals(sourceMap, spec, Side.SOURCE);
        Map<String, BigDecimal> rightTotals = totals(targetMap, spec, Side.TARGET);

        // Field diffs
        List<FieldDiff> diffs = compare(matched, sourceMap, targetMap, spec);

        double coverage = matched.size() * 100.0 / Math.max(1, Math.max(sourceKeys.size(), targetKeys.size()));

        List<com.fiserv.optis.qarecon.model.Pair<Document, Document>> matchedPairs = matched.stream()
                .map(k -> new com.fiserv.optis.qarecon.model.Pair<>(sourceMap.get(k), targetMap.get(k)))
                .collect(Collectors.toList());

        return new ReconciliationResult(
                sourceMap.size(), targetMap.size(),
                matched.size(), coverage,
                sourceOnly, targetOnly,
                leftTotals, rightTotals,
                diffs,
                matchedPairs,
                Instant.now()
        );
    }

    public ReconciliationResult runBalanceCheck(
            List<Document> sourceDocs,
            List<Document> targetDocs,
            ReconciliationSpec spec,
            List<String> sourceBalanceFields,
            List<String> targetBalanceFields,
            BigDecimal tolerance
    ) {
        String runId = spec.runId != null ? spec.runId : String.valueOf(System.currentTimeMillis());
        spec.runId = runId;

        Map<String, Document> sourceMap = toMap(sourceDocs, spec, Side.SOURCE);

        // Group target docs by join key (may be multiple per key)
        Map<String, List<Document>> targetGrouped = targetDocs.stream()
                .collect(Collectors.groupingBy(
                        d -> spec.joinKeys.stream()
                                .map(k -> String.valueOf(d.get(k.targetField)))
                                .collect(Collectors.joining("|"))
                ));

        Set<String> matched = new HashSet<>(sourceMap.keySet());
        matched.retainAll(targetGrouped.keySet());

        Set<String> sourceOnly = new HashSet<>(sourceMap.keySet());
        sourceOnly.removeAll(targetGrouped.keySet());
        Set<String> targetOnly = new HashSet<>(targetGrouped.keySet());
        targetOnly.removeAll(sourceMap.keySet());

        List<FieldDiff> diffs = new ArrayList<>();
        List<Pair<Document, Document>> matchedPairs = new ArrayList<>();

        for (String key : matched) {
            Document src = sourceMap.get(key);
            List<Document> tgtList = targetGrouped.get(key);

            BigDecimal srcSum = sourceBalanceFields.stream()
                    .map(f -> {
                        Object v = getNestedValue(src, f);
                        if (v == null) return BigDecimal.ZERO;
                        try {
                            return new BigDecimal(v.toString());
                        } catch (Exception e) {
                            return BigDecimal.ZERO;
                        }
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal tgtSum = tgtList.stream()
                    .flatMap(tgt -> targetBalanceFields.stream()
                            .map(f -> {
                                Object v = getNestedValue(tgt, f);
                                if (v == null) return BigDecimal.ZERO;
                                try {
                                    return new BigDecimal(v.toString());
                                } catch (Exception e) {
                                    return BigDecimal.ZERO;
                                }
                            }))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (srcSum.subtract(tgtSum).abs().compareTo(tolerance) > 0) {
                diffs.add(FieldDiff.number(key, "sourceSum", "targetSum", srcSum, tgtSum, srcSum.subtract(tgtSum).abs(), tolerance));
            }

            // Optionally, pair source with a synthetic target doc holding the sum
            Document syntheticTarget = new Document();
            syntheticTarget.put("_groupedSum", tgtSum);
            matchedPairs.add(new Pair<>(src, syntheticTarget));
        }

        // Calculate match coverage percentage
        double coverage = matched.size() * 100.0 / Math.max(1, Math.max(sourceMap.size(), targetGrouped.size()));

        // Totals for reporting
        BigDecimal totalSource = sourceMap.values().stream()
                .flatMap(doc -> sourceBalanceFields.stream().map(f -> toBigDecimal(doc.get(f))))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTarget = targetGrouped.values().stream()
                .flatMap(list -> list.stream())
                .flatMap(doc -> targetBalanceFields.stream().map(f -> toBigDecimal(doc.get(f))))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> leftTotals = new HashMap<>();
        leftTotals.put("balanceSum", totalSource);
        Map<String, BigDecimal> rightTotals = new HashMap<>();
        rightTotals.put("balanceSum", totalTarget);

        return new ReconciliationResult(
                sourceMap.size(), targetGrouped.size(),
                matched.size(), coverage,
                sourceOnly, targetOnly,
                leftTotals, rightTotals,
                diffs,
                matchedPairs,
                java.time.Instant.now()
        );
    }

    // Add this to ReconciliationEngine.java

    public ReconciliationResult runEnhancedBalanceCheck(
            List<Document> sourceDocs,
            List<Document> targetDocs,
            ReconciliationSpec spec,
            List<BalanceFieldConfig> sourceConfigs,
            List<BalanceFieldConfig> targetConfigs,
            BigDecimal tolerance
    ) {
        String runId = spec.runId != null ? spec.runId : String.valueOf(System.currentTimeMillis());
        spec.runId = runId;

        // Group and aggregate source documents
        Map<String, Document> sourceAggregated = groupAndAggregate(sourceDocs, sourceConfigs, spec, Side.SOURCE);

        // Group and aggregate target documents
        Map<String, Document> targetAggregated = groupAndAggregate(targetDocs, targetConfigs, spec, Side.TARGET);

        // Find matched keys
        Set<String> matched = new HashSet<>(sourceAggregated.keySet());
        matched.retainAll(targetAggregated.keySet());

        Set<String> sourceOnly = new HashSet<>(sourceAggregated.keySet());
        sourceOnly.removeAll(targetAggregated.keySet());

        Set<String> targetOnly = new HashSet<>(targetAggregated.keySet());
        targetOnly.removeAll(sourceAggregated.keySet());

        List<FieldDiff> diffs = new ArrayList<>();
        List<Pair<Document, Document>> matchedPairs = new ArrayList<>();

        // Compare aggregated values
        for (String key : matched) {
            Document srcDoc = sourceAggregated.get(key);
            Document tgtDoc = targetAggregated.get(key);

            // Extract aggregated balance values
            BigDecimal srcSum = extractAggregatedBalance(srcDoc, sourceConfigs);
            BigDecimal tgtSum = extractAggregatedBalance(tgtDoc, targetConfigs);

            if (srcSum.subtract(tgtSum).abs().compareTo(tolerance) > 0) {
                diffs.add(FieldDiff.number(key, "sourceBalanceSum", "targetBalanceSum",
                        srcSum, tgtSum, srcSum.subtract(tgtSum).abs(), tolerance));
            }

            matchedPairs.add(new Pair<>(srcDoc, tgtDoc));
        }

        double coverage = matched.size() * 100.0 / Math.max(1, Math.max(sourceAggregated.size(), targetAggregated.size()));

        // Calculate totals
        BigDecimal totalSource = sourceAggregated.values().stream()
                .map(doc -> extractAggregatedBalance(doc, sourceConfigs))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTarget = targetAggregated.values().stream()
                .map(doc -> extractAggregatedBalance(doc, targetConfigs))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> leftTotals = new HashMap<>();
        leftTotals.put("balanceSum", totalSource);

        Map<String, BigDecimal> rightTotals = new HashMap<>();
        rightTotals.put("balanceSum", totalTarget);

        return new ReconciliationResult(
                sourceAggregated.size(), targetAggregated.size(),
                matched.size(), coverage,
                sourceOnly, targetOnly,
                leftTotals, rightTotals,
                diffs,
                matchedPairs,
                java.time.Instant.now()
        );
    }

    private Map<String, Document> groupAndAggregate(
            List<Document> docs,
            List<BalanceFieldConfig> configs,
            ReconciliationSpec spec,
            Side side
    ) {
        if (configs.isEmpty()) {
            return Collections.emptyMap();
        }

        // Determine grouping fields (use first config's groupBy or join keys)
        List<String> groupByFields = configs.get(0).getGroupByFields();
        if (groupByFields == null || groupByFields.isEmpty()) {
            // Fall back to join keys
            groupByFields = spec.joinKeys.stream()
                    .map(k -> side == Side.SOURCE ? k.sourceField : k.targetField)
                    .collect(Collectors.toList());
        }

        final List<String> finalGroupByFields = groupByFields;

        // Group documents by key with null-safe handling
        Map<String, List<Document>> grouped = docs.stream()
                .collect(Collectors.groupingBy(doc ->
                        createGroupingKey(doc, finalGroupByFields)
                ));

        // Aggregate each group
        Map<String, Document> result = new HashMap<>();
        for (Map.Entry<String, List<Document>> entry : grouped.entrySet()) {
            String key = entry.getKey();
            List<Document> group = entry.getValue();

            Document aggregated = new Document();

            // Store the grouping key fields in the aggregated document
            for (String field : finalGroupByFields) {
                Object value = getNestedValue(group.get(0), field);
                aggregated.put(field, value);
            }

            // Store the original grouping key for matching
            aggregated.put("_groupingKey", key);

            // Aggregate balance fields according to their strategies
            for (BalanceFieldConfig config : configs) {
                String fieldName = config.getFieldName();
                AggregationStrategy strategy = config.getAggregationStrategy();

                List<BigDecimal> values = group.stream()
                        .map(doc -> toBigDecimal(getNestedValue(doc, fieldName)))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                BigDecimal aggregatedValue = applyAggregation(values, strategy);
                aggregated.put(fieldName + "_aggregated", aggregatedValue);
            }

            // Store count for reference
            aggregated.put("_groupCount", group.size());

            result.put(key, aggregated);
        }

        return result;
    }

    private String createGroupingKey(Document doc, List<String> fields) {
        List<Object> values = fields.stream()
                .map(field -> getNestedValue(doc, field))
                .collect(Collectors.toList());

        // Option 1: Use hash for very long keys
        if (shouldUseHash(values)) {
            return String.valueOf(Objects.hash(values.toArray()));
        }

        // Option 2: Standard delimiter-based key
        return values.stream()
                .map(v -> v != null ? String.valueOf(v).replace("|", "\\|") : "<<NULL>>")
                .collect(Collectors.joining("|"));
    }

    private boolean shouldUseHash(List<Object> values) {
        // Use hash if the combined key would be very long
        int estimatedLength = values.stream()
                .mapToInt(v -> v != null ? v.toString().length() : 6)
                .sum();
        return estimatedLength > 500; // Threshold
    }

    private BigDecimal applyAggregation(List<BigDecimal> values, AggregationStrategy strategy) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }

        switch (strategy) {
            case SUM:
                return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            case FIRST:
                return values.get(0);
            case LAST:
                return values.get(values.size() - 1);
            case MIN:
                return values.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            case MAX:
                return values.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            case AVG:
                BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                return sum.divide(BigDecimal.valueOf(values.size()), 2, java.math.RoundingMode.HALF_UP);
            case COUNT:
                return BigDecimal.valueOf(values.size());
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal extractAggregatedBalance(Document doc, List<BalanceFieldConfig> configs) {
        return configs.stream()
                .map(config -> {
                    Object value = doc.get(config.getFieldName() + "_aggregated");
                    return toBigDecimal(value);
                })
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    private List<Document> fetch(MongoClient client, ReconciliationSpec spec, Side side) {
        String coll = side == Side.SOURCE ? spec.sourceCollection : spec.targetCollection;
        if (coll == null) throw new IllegalArgumentException("Missing collection for " + side);

        String[] parts = coll.split("\\.", 2);
        String dbName = parts[0];
        String collName = parts[1];

        MongoDatabase db = client.getDatabase(dbName);
        MongoCollection<Document> c = db.getCollection(collName);

        Document filter = side == Side.SOURCE ? (spec.sourceFilter != null ? spec.sourceFilter : new Document()) :
                (spec.targetFilter != null ? spec.targetFilter : new Document());

        // Basic find, for 1:M you could pre-aggregate (leftAgg/rightAgg), omitted for brevity
        List<Document> out = new ArrayList<>();
        try (MongoCursor<Document> cur = c.find(filter).batchSize(1000).iterator()) {
            while (cur.hasNext()) out.add(cur.next());
        }
        return out;
    }

    private Map<String, Document> toMap(List<Document> docs, ReconciliationSpec spec, Side side) {
        List<JoinKey> keys = spec.joinKeys;
        Function<Document, String> keyFn = d -> keys.stream()
                .map(k -> {
                    if (side == Side.SOURCE) {
                        return String.valueOf(d.get(k.sourceField));
                    } else {
                        return String.valueOf(d.get(k.targetField));
                    }
                })
                .collect(Collectors.joining("|"));
        return docs.stream().collect(Collectors.toMap(keyFn, Function.identity(), (a, b) -> a, ConcurrentHashMap::new));
    }

    private Map<String, BigDecimal> totals(Map<String, Document> map, ReconciliationSpec spec, Side side) {
        Map<String, BigDecimal> totals = new HashMap<>();
        for (FieldMapping fm : spec.mappings) {
            if (!"number".equalsIgnoreCase(fm.getCompareAs())) continue;
            String f = side == Side.SOURCE ? fm.getLeftField() : fm.getRightField();
            BigDecimal sum = map.values().stream()
                    .map(d -> toBigDecimal(d.get(f)))
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totals.put(f, sum);
        }
        return totals;
    }

    private List<FieldDiff> compare(Set<String> matched,
                                    Map<String, Document> left,
                                    Map<String, Document> right,
                                    ReconciliationSpec spec) {
        List<FieldDiff> out = new ArrayList<>();
        for (String k : matched) {
            Document l = left.get(k);
            Document r = right.get(k);
            for (FieldMapping fm : spec.mappings) {
                switch (fm.getCompareAs().toLowerCase()) {
                    case "number": {
                        BigDecimal lt = toBigDecimal(l.get(fm.getLeftField()));
                        BigDecimal rt = toBigDecimal(r.get(fm.getRightField()));
                        BigDecimal tol = fm.numericToleranceOr(spec.defaultNumericTolerance);
                        if (!numEq(lt, rt, tol)) {
                            out.add(FieldDiff.number(k, fm.getLeftField(), fm.getRightField(), lt, rt, lt == null || rt == null ? null : lt.subtract(rt).abs(), tol));
                        }
                        break;
                    }
                    case "datetime": {
                        Instant li = toInstant(l.get(fm.getLeftField()));
                        Instant ri = toInstant(r.get(fm.getRightField()));
                        Duration tol = fm.timeToleranceOr(spec.defaultTimeTolerance);
                        if (!timeEq(li, ri, tol)) {
                            out.add(FieldDiff.time(k, fm.getLeftField(), fm.getRightField(), li, ri, tol));
                        }
                        break;
                    }
                    default: {
                        String ls = toString(l.get(fm.getLeftField()));
                        String rs = toString(r.get(fm.getRightField()));
                        if (!Objects.equals(ls, rs)) {
                            out.add(FieldDiff.string(k, fm.getLeftField(), fm.getRightField(), ls, rs));
                        }
                        break;
                    }
                }
            }
        }
        return out;
    }

    private static boolean numEq(BigDecimal a, BigDecimal b, BigDecimal tol) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.subtract(b).abs().compareTo(tol) <= 0;
    }

    private static boolean timeEq(Instant a, Instant b, Duration tol) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        long diff = Math.abs(a.toEpochMilli() - b.toEpochMilli());
        return diff <= tol.toMillis();
    }

    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) return null;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(((Number) v).toString());
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private static Instant toInstant(Object v) {
        if (v == null) return null;
        if (v instanceof java.util.Date) return ((java.util.Date) v).toInstant();
        try {
            return Instant.parse(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private static String toString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private Object getNestedValue(Document doc, String fieldPath) {
        if (doc == null || fieldPath == null || fieldPath.isEmpty()) {
            return null;
        }

        Object value = doc;
        String[] keys = fieldPath.split("\\.");

        for (String key : keys) {
            if (value instanceof Document) {
                value = ((Document) value).get(key);
                if (value == null) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return value;
    }

    // ————————— Profile loader —————————
    public static class Profiles {

        public static MongoProfile load(String name) {
            try {
                String path = "/profiles/" + name + ".yaml";
                InputStream in = ReconciliationEngine.class.getResourceAsStream(path);
                if (in == null) throw new IllegalArgumentException("Profile not found: " + path);
                ObjectMapper om = new ObjectMapper(new YAMLFactory());
                return om.readValue(in, MongoProfile.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public static Clients connect(MongoProfile p) {
            MongoClientSettings.Builder lb = MongoClientSettings.builder().applyConnectionString(new ConnectionString(p.mongoUriLeft));
            MongoClientSettings.Builder rb = MongoClientSettings.builder().applyConnectionString(new ConnectionString(p.mongoUriRight));

            if (p.readPreference != null) {
                lb.readPreference(ReadPreference.valueOf(p.readPreference));
                rb.readPreference(ReadPreference.valueOf(p.readPreference));
            }

            if (p.readConcern != null) {
                lb.readConcern(parseReadConcern(p.readConcern));
                rb.readConcern(parseReadConcern(p.readConcern));
            }

            MongoClient l = MongoClients.create(lb.build());
            MongoClient r = MongoClients.create(rb.build());
            return new Clients(l, r);
        }
    }

    public static final class Clients implements AutoCloseable {
        public final MongoClient source;
        public final MongoClient target;

        public Clients(MongoClient source, MongoClient target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public void close() {
            try {
                source.close();
            } finally {
                target.close();
            }
        }
    }

    private static ReadConcern parseReadConcern(String rc) {
        if (rc == null) return null;
        switch (rc.toUpperCase()) {
            case "LOCAL": return ReadConcern.LOCAL;
            case "MAJORITY": return ReadConcern.MAJORITY;
            case "LINEARIZABLE": return ReadConcern.LINEARIZABLE;
            case "AVAILABLE": return ReadConcern.AVAILABLE;
            case "SNAPSHOT": return ReadConcern.SNAPSHOT;
            default: throw new IllegalArgumentException("Unknown ReadConcern: " + rc);
        }
    }

    // ————————— Result DTO —————————
    public static final class ReconciliationResult {
        public int sourceCount;
        public int targetCount;
        public int matchedCount;
        public double matchCoveragePercent;
        public Set<String> leftOnlyKeys;
        public Set<String> rightOnlyKeys;
        public Map<String, BigDecimal> leftTotals;
        public Map<String, BigDecimal> rightTotals;
        public List<FieldDiff> fieldDiffs;
        public List<Pair<Document, Document>> matchedPairs;
        public final Instant generatedAt;

        public ReconciliationResult() {
            this.sourceCount = 0;
            this.targetCount = 0;
            this.matchedCount = 0;
            this.matchCoveragePercent = 0.0;
            this.leftOnlyKeys = new java.util.HashSet<>();
            this.rightOnlyKeys = new java.util.HashSet<>();
            this.leftTotals = new java.util.HashMap<>();
            this.rightTotals = new java.util.HashMap<>();
            this.fieldDiffs = new java.util.ArrayList<>();
            this.matchedPairs = new java.util.ArrayList<>();
            this.generatedAt = java.time.Instant.now();
        }

        public ReconciliationResult(int leftCount, int rightCount, int matchedCount, double coverage,
                                    Set<String> leftOnlyKeys, Set<String> rightOnlyKeys,
                                    Map<String, BigDecimal> leftTotals, Map<String, BigDecimal> rightTotals,
                                    List<FieldDiff> diffs,
                                    List<Pair<Document, Document>> matchedPairs,
                                    Instant when) {
            this.sourceCount = leftCount;
            this.targetCount = rightCount;
            this.matchedCount = matchedCount;
            this.matchCoveragePercent = coverage;
            this.leftOnlyKeys = leftOnlyKeys;
            this.rightOnlyKeys = rightOnlyKeys;
            this.leftTotals = leftTotals;
            this.rightTotals = rightTotals;
            this.fieldDiffs = diffs;
            this.matchedPairs = matchedPairs;
            this.generatedAt = when;
        }
    }

    // ————————— Field diff DTO —————————
    public static final class FieldDiff {
        public final String key;
        public final String leftField;
        public final String rightField;
        public final String type;
        public final Object leftValue;
        public final Object rightValue;
        public final Object delta;
        public final Object tolerance;

        private FieldDiff(String key, String lf, String rf, String type, Object lv, Object rv, Object delta, Object tol) {
            this.key = key;
            this.leftField = lf;
            this.rightField = rf;
            this.type = type;
            this.leftValue = lv;
            this.rightValue = rv;
            this.delta = delta;
            this.tolerance = tol;
        }

        public static FieldDiff number(String key, String lf, String rf, Object lv, Object rv, Object delta, Object tol) {
            return new FieldDiff(key, lf, rf, "number", lv, rv, delta, tol);
        }

        public static FieldDiff time(String key, String lf, String rf, Object lv, Object rv, Object tol) {
            return new FieldDiff(key, lf, rf, "datetime", lv, rv, null, tol);
        }

        public static FieldDiff string(String key, String lf, String rf, Object lv, Object rv) {
            return new FieldDiff(key, lf, rf, "string", lv, rv, null, null);
        }
    }
}
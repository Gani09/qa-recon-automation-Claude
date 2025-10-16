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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.io.InputStream;

public final class ReconciliationEngine {

    private final MongoClient source;
    private final MongoClient target;

    public ReconciliationEngine(MongoClient source, MongoClient target) {
        this.source = source; this.target = target;
    }

    public ReconciliationResult run(ReconciliationSpec spec) {
        String runId = spec.runId != null ? spec.runId : String.valueOf(System.currentTimeMillis());
        spec.runId = runId;

        List<Document> leftDocs = fetch(source, spec.sourceCollection, spec.sourceFilter);
        List<Document> rightDocs = fetch(target, spec.targetCollection, spec.targetFilter);

        Map<String, Document> leftMap = toMap(leftDocs, spec.joinKeys, Side.SOURCE);
        Map<String, Document> rightMap = toMap(rightDocs, spec.joinKeys, Side.TARGET);

        Set<String> leftKeys = leftMap.keySet();
        Set<String> rightKeys = rightMap.keySet();

        Set<String> matched = new HashSet<>(leftKeys); matched.retainAll(rightKeys);
        Set<String> leftOnly = new HashSet<>(leftKeys); leftOnly.removeAll(rightKeys);
        Set<String> rightOnly = new HashSet<>(rightKeys); rightOnly.removeAll(leftKeys);

        Map<String, BigDecimal> leftTotals = totals(leftMap, spec.mappings, Side.SOURCE);
        Map<String, BigDecimal> rightTotals = totals(rightMap, spec.mappings, Side.TARGET);

        List<FieldDiff> diffs = compare(matched, leftMap, rightMap, spec.mappings, spec.defaultNumericTolerance, spec.defaultTimeTolerance);

        double coverage = matched.size() * 100.0 / Math.max(1, Math.max(leftKeys.size(), rightKeys.size()));

        List<Pair<Document, Document>> matchedPairs = matched.stream().map(k -> new Pair<>(leftMap.get(k), rightMap.get(k))).collect(Collectors.toList());

        return new ReconciliationResult(leftMap.size(), rightMap.size(), matched.size(), coverage, leftOnly, rightOnly,
                leftTotals, rightTotals, diffs, matchedPairs, Instant.now());
    }

    private List<Document> fetch(MongoClient client, String coll, Document filter) {
        if (coll == null) throw new IllegalArgumentException("Missing collection");
        String[] parts = coll.split("\.", 2);
        MongoCollection<Document> c = client.getDatabase(parts[0]).getCollection(parts[1]);
        List<Document> out = new ArrayList<>();
        try (MongoCursor<Document> cur = c.find(filter != null ? filter : new Document()).iterator()) {
            while (cur.hasNext()) out.add(cur.next());
        }
        return out;
    }

    private Map<String, Document> toMap(List<Document> docs, List<JoinKey> keys, Side side) {
        Function<Document, String> keyFn = d -> keys.stream()
            .map(k -> String.valueOf(side==Side.SOURCE ? d.get(k.sourceField) : d.get(k.targetField)))
            .collect(Collectors.joining("|"));
        return docs.stream().collect(Collectors.toMap(keyFn, Function.identity(), (a,b)->a, ConcurrentHashMap::new));
    }

    private Map<String, BigDecimal> totals(Map<String, Document> map, List<FieldMapping> mappings, Side side) {
        Map<String, BigDecimal> totals = new HashMap<>();
        for (FieldMapping fm : mappings) {
            if (!"number".equalsIgnoreCase(fm.getCompareAs())) continue;
            String f = side==Side.SOURCE ? fm.getLeftField() : fm.getRightField();
            BigDecimal sum = map.values().stream().map(d -> toBigDecimal(d.get(f)))
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            totals.put(f, sum);
        }
        return totals;
    }

    private List<FieldDiff> compare(Set<String> matched, Map<String, Document> left, Map<String, Document> right,
                                    List<FieldMapping> mappings, BigDecimal defaultNumTol, Duration defaultTimeTol) {
        List<FieldDiff> out = new ArrayList<>();
        for (String k : matched) {
            Document l = left.get(k); Document r = right.get(k);
            for (FieldMapping fm : mappings) {
                String type = String.valueOf(fm.getCompareAs()).toLowerCase();
                switch (type) {
                    case "number" -> {
                        BigDecimal lt = toBigDecimal(l.get(fm.getLeftField()));
                        BigDecimal rt = toBigDecimal(r.get(fm.getRightField()));
                        BigDecimal tol = fm.numericToleranceOr(defaultNumTol);
                        if (!numEq(lt, rt, tol)) {
                            out.add(FieldDiff.number(k, fm.getLeftField(), fm.getRightField(), lt, rt,
                                    (lt==null || rt==null) ? null : lt.subtract(rt).abs(), tol));
                        }
                    }
                    default -> {
                        String ls = Objects.toString(l.get(fm.getLeftField()), "");
                        String rs = Objects.toString(r.get(fm.getRightField()), "");
                        if (!Objects.equals(ls, rs)) out.add(FieldDiff.string(k, fm.getLeftField(), fm.getRightField(), ls, rs));
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
    private static BigDecimal toBigDecimal(Object v){
        if (v == null) return null;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return new BigDecimal(n.toString());
        try { return new BigDecimal(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    public static class FieldDiff {
        public final String key, leftField, rightField, type;
        public final Object leftValue, rightValue, delta, tolerance;
        private FieldDiff(String key, String lf, String rf, String type, Object lv, Object rv, Object delta, Object tol){
            this.key=key; this.leftField=lf; this.rightField=rf; this.type=type; this.leftValue=lv; this.rightValue=rv; this.delta=delta; this.tolerance=tol;
        }
        public static FieldDiff number(String key, String lf, String rf, Object lv, Object rv, Object delta, Object tol){ return new FieldDiff(key,lf,rf,"number",lv,rv,delta,tol); }
        public static FieldDiff string(String key, String lf, String rf, Object lv, Object rv){ return new FieldDiff(key,lf,rf,"string",lv,rv,null,null); }
    }

    public static final class ReconciliationResult {
        public int sourceCount;
        public int targetCount;
        public int matchedCount;
        public double matchCoveragePercent;
        public Set<String> leftOnlyKeys;
        public Set<String> rightOnlyKeys;
        public Map<String, java.math.BigDecimal> leftTotals;
        public Map<String, java.math.BigDecimal> rightTotals;
        public java.util.List<FieldDiff> fieldDiffs;
        public java.util.List<com.fiserv.optis.qarecon.model.Pair<org.bson.Document, org.bson.Document>> matchedPairs;
        public final Instant generatedAt;

        public ReconciliationResult(int leftCount, int rightCount, int matchedCount, double coverage,
                                    Set<String> leftOnlyKeys, Set<String> rightOnlyKeys,
                                    Map<String, java.math.BigDecimal> leftTotals, Map<String, java.math.BigDecimal> rightTotals,
                                    java.util.List<FieldDiff> diffs, java.util.List<com.fiserv.optis.qarecon.model.Pair<org.bson.Document, org.bson.Document>> matchedPairs,
                                    Instant when) {
            this.sourceCount = leftCount; this.targetCount = rightCount; this.matchedCount = matchedCount;
            this.matchCoveragePercent = coverage; this.leftOnlyKeys = leftOnlyKeys; this.rightOnlyKeys = rightOnlyKeys;
            this.leftTotals = leftTotals; this.rightTotals = rightTotals; this.fieldDiffs = diffs; this.matchedPairs = matchedPairs;
            this.generatedAt = when;
        }
    }

    public static class Profiles {
        public static MongoProfile load(String name) {
            try (InputStream in = ReconciliationEngine.class.getResourceAsStream("/profiles/" + name + ".yaml")) {
                if (in == null) throw new IllegalArgumentException("Profile not found: " + name);
                return new ObjectMapper(new YAMLFactory()).readValue(in, MongoProfile.class);
            } catch (Exception e) { throw new RuntimeException(e); }
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
            return new Clients(MongoClients.create(lb.build()), MongoClients.create(rb.build()));
        }
    }
    public static final class Clients implements AutoCloseable {
        public final MongoClient source; public final MongoClient target;
        public Clients(MongoClient source, MongoClient target){ this.source=source; this.target=target; }
        @Override public void close(){ try { source.close(); } finally { target.close(); } }
    }
    private static ReadConcern parseReadConcern(String rc){
        if (rc == null) return null;
        return switch (rc.toUpperCase()) {
            case "LOCAL" -> ReadConcern.LOCAL;
            case "MAJORITY" -> ReadConcern.MAJORITY;
            case "LINEARIZABLE" -> ReadConcern.LINEARIZABLE;
            case "AVAILABLE" -> ReadConcern.AVAILABLE;
            case "SNAPSHOT" -> ReadConcern.SNAPSHOT;
            default -> throw new IllegalArgumentException("Unknown ReadConcern: " + rc);
        };
    }
}

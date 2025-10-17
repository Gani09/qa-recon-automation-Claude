package com.fiserv.optis.qarecon.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiserv.optis.qarecon.constants.ReportsContext;
import com.fiserv.optis.qarecon.engine.ReconciliationEngine;
import com.fiserv.optis.qarecon.engine.ReconciliationEngine.ReconciliationResult;
import com.fiserv.optis.qarecon.model.entities.ReconConfigEntity;
import com.fiserv.optis.qarecon.model.*;
import com.fiserv.optis.qarecon.report.model.GenericPojo;
import com.fiserv.optis.qarecon.service.CryptService;
import com.fiserv.optis.qarecon.service.MongoService;
import com.fiserv.optis.qarecon.util.ReportWriter;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Scenario;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class StepDefinitions {

    private List<Map<String, String>> fieldMappings;
    private List<String> balanceFields;

    private final MongoService mongoService = new MongoService();
    private final CryptService cryptService = new CryptService();

    public static Integer counter = 0;

    private Scenario scenario;

    public MongoService service = new MongoService();

    @Before
    public void before(Scenario scenario) {
        this.scenario = scenario;
    }

    private static class Ctx {
        ReconciliationSpec spec = new ReconciliationSpec();
        ReconciliationResult result;
        ReconciliationEngine.Clients clients;
        long resultCount;
        List<Document> sourceDocs;
        List<Document> targetDocs;
        Map<String, Object> extractedSourceValues;
        Map<String, Object> extractedTargetValues;
    }

    private final Ctx ctx = new Ctx();

    @Given("I use Mongo profile {string}")
    public void useProfile(String name) {
        service.name = name;
        ctx.spec.profileName = name;
        MongoProfile p = ReconciliationEngine.Profiles.load(name);
        ctx.clients = ReconciliationEngine.Profiles.connect(p);
    }

    @Given("default numeric tolerance is {double}")
    public void defaultNumericTol(double tol) {
        ctx.spec.defaultNumericTolerance = BigDecimal.valueOf(tol);
    }

    @Given("default time tolerance is {string}")
    public void defaultTimeTol(String isoDur) {
        ctx.spec.defaultTimeTolerance = Duration.parse(isoDur);
    }

    @Given("configuration {string} from {string}")
    public void setExcelFileAsConfiguration(String configName, String configTable) {
        String[] parts = configTable.split( "\\.",  2);
        String dbName = parts[0];
        String collName = parts[1];
        Document filter = new Document("configName", configName);
        Document doc = ctx.clients.source
                .getDatabase(dbName)
                .getCollection(collName)
                .find(filter)
                .first();
        ReconConfigEntity config = null;
        if (doc != null) {
            ObjectMapper mapper = new ObjectMapper();
            config = mapper.convertValue(doc, ReconConfigEntity.class);
        }
        if (config == null) {
            throw new IllegalArgumentException("No configuration found with name: " + configName);
        }
        ctx.spec.configEntity = config;
    }

    @Given("source collection {string}")
    public void sourceOnly(String coll) {
        ctx.spec.sourceCollection = coll;
    }

    @Given("target collection {string}")
    public void targetOnly(String coll) {
        ctx.spec.targetCollection = coll;
    }

    @Given("source collection {string} filtered by:")
    public void sourceWithFilter(String coll, String json) {
        ctx.spec.sourceCollection = coll;
        ctx.spec.sourceFilter = Document.parse(json);
    }

    @Given("target collection {string} filtered by:")
    public void targetWithFilter(String coll, String json) {
        ctx.spec.targetCollection = coll;
        ctx.spec.targetFilter = Document.parse(json);
    }

    @Given("join keys:")
    public void joinKeys(DataTable table) {
        ctx.spec.joinKeys = table.asMaps().stream()
                .map(JoinKey::of)
                .collect(Collectors.toList());
    }

    @Given("field mappings:")
    public void fieldMappings(DataTable table) {
        ctx.spec.mappings = table.asMaps().stream()
                .map(FieldMapping::from).collect(Collectors.toList());
    }

    @When("I run the reconciliation with mode {string}")
    public void runRecon(String mode) {
        ctx.spec.mode = Mode.valueOf(mode);
        try (ReconciliationEngine.Clients c = ctx.clients) {
            ReconciliationEngine engine = new ReconciliationEngine(c.source, c.target);
            ctx.result = engine.run(ctx.spec);
        }
    }

    @Then("record counts should match")
    public void countsMatch() {
        try {
            Assertions.assertThat(ctx.result.sourceCount).isEqualTo(ctx.result.targetCount);

        } catch (AssertionError e) {
            int count = ++counter;

            GenericPojo gpl = GenericPojo.builder().uniqueId(count).srcOrTar('S').collectionName(ctx.spec.sourceCollection).totalCount(ctx.result.sourceCount).build();
            GenericPojo gpr = GenericPojo.builder().uniqueId(count).srcOrTar('T').collectionName(ctx.spec.targetCollection).totalCount(ctx.result.targetCount).build();
            //ReportsContext.genericPojoMap.computeIfAbsent(scenario.getName(), k → new java.util.ArrayList<>()).addAll(java.util.Arrays.asList(gpl, gpr));
            if (ReportsContext.genericPojoMapForFailureWithKey == null) {
                ReportsContext.genericPojoMapForFailureWithKey = new HashMap<>();
            }

            ReportsContext.genericPojoMapForFailureWithKey.computeIfAbsent(scenario.getName(), k ->new java.util.HashMap<>())
                    .computeIfAbsent("record counts should match", k -> new java.util.ArrayList<>())
                    .addAll(java.util.Arrays.asList(gpl, gpr));
            throw new AssertionError( "step failed due to mismatch in record count");
            //throw new AssertionError("step failed due to mismatch in field values");
        }
    }

    @Then("total of {string} on left equals total of {string} on right within {double}")
    public void totalsWithin(String left, String right, double tol) {
        BigDecimal l = ctx.result.leftTotals.get(left);
        BigDecimal r = ctx.result.rightTotals.get(right);
        BigDecimal T = BigDecimal.valueOf(tol);
        Assertions.assertThat(l.subtract(r).abs()).isLessThanOrEqualTo(T);
    }

    @Then("there should be no left-only records")
    public void noLeftOnly() {
        Assertions.assertThat(ctx.result.leftOnlyKeys).isEmpty();
    }

    @Then("there should be no right-only records")
    public void noRightOnly() {
        Assertions.assertThat(ctx.result.rightOnlyKeys).isEmpty();
    }

    @Then("matched keys coverage should be ≥ {double}%")
    public void coverage(Double pct) {
        Assertions.assertThat(ctx.result.matchCoveragePercent).isGreaterThanOrEqualTo(pct);
    }

    @Then("export unmatched sets and differences to {string}")
    public void export(String path) throws IOException {
        ReportWriter.writeAll(ctx.spec, ctx.result, path);
    }

    @Given("source collection {string} is filtered by excel sheet {string}")
    public void source_collection_filtered_by_excel_sheet(String coll, String sheet) {
        if (ctx.spec.configEntity == null || ctx.spec.configEntity.getFilters() == null) {
            throw new IllegalStateException("ReconConfigEntity or its filters are not loaded.");
        }
        Object sourceFilterObj = ctx.spec.configEntity.getFilters().get("sourceFilter");
        if (sourceFilterObj == null) {
            throw new IllegalArgumentException("No source filter found in ReconConfigEntity.");
        }
        ctx.spec.sourceCollection = coll;
        if (sourceFilterObj instanceof Document) {
            ctx.spec.sourceFilter = (Document) sourceFilterObj;
        } else {
            ctx.spec.sourceFilter = new Document((Map<String, Object>) sourceFilterObj);
        }
    }

    @Given("target collection {string} is filtered by excel sheet {string}")
    public void target_collection_filtered_by_excel_sheet(String coll, String sheet) {
        if (ctx.spec.configEntity == null || ctx.spec.configEntity.getFilters() == null) {
            throw new IllegalStateException("ReconConfigEntity or its filters are not loaded.");
        }
        Object targetFilterObj = ctx.spec.configEntity.getFilters().get("targetFilter");
        if (targetFilterObj == null) {
            throw new IllegalArgumentException("No target filter found in ReconConfigEntity.");
        }
        ctx.spec.targetCollection = coll;
        if (targetFilterObj instanceof Document) {
            ctx.spec.targetFilter = (Document) targetFilterObj;
        } else {
            ctx.spec.targetFilter = new Document((Map<String, Object>) targetFilterObj);
        }
    }

    @Given("join keys from excel sheet {string}")
    public void joinKeysFromExcelSheet(String sheetName) {
        if (ctx.spec.configEntity == null || ctx.spec.configEntity.getJoinKeys() == null) {
            throw new IllegalStateException("ReconConfigEntity or its join keys are not loaded.");
        }
        ctx.spec.joinKeys = ctx.spec.configEntity.getJoinKeys();
    }

    @Given("field mappings between source and target from excel sheet {string}")
    public void fieldMappingsFromExcelSheet(String sheetName) {
        if (ctx.spec.configEntity == null || ctx.spec.configEntity.getFieldMappings() == null) {
            throw new IllegalStateException("ReconConfigEntity or its field mappings are not loaded.");
        }
        ctx.spec.mappings = ctx.spec.configEntity.getFieldMappings();
    }

    @Then("for each matched record, all the source fields should match target fields")
    public void validateFieldMappings() {
        if (ctx.spec.mappings == null || ctx.spec.mappings.isEmpty()) {
            throw new IllegalStateException("Field mappings are not defined.");
        }
        Map<String,String> keysMap = ctx.spec.mappings.stream().collect(Collectors.toMap(mapping-> mapping.getLeftField(),mapping-> mapping.getRightField()));
        for (Pair<Document, Document> pair : ctx.result.matchedPairs) {
            int count = ++counter;
            Document left = pair.getLeft();
            Document right = pair.getRight();

            GenericPojo gpl = GenericPojo.builder().uniqueId(count).srcOrTar('S').collectionName(ctx.spec.sourceCollection).totalCount(ctx.result.sourceCount).build();
            GenericPojo gpr = GenericPojo.builder().uniqueId(count).srcOrTar('T').collectionName(ctx.spec.targetCollection).totalCount(ctx.result.targetCount).build();

            for (String key : left.keySet()) {
                if(keysMap.keySet().contains(key)){
                    Object leftValue = left.get(key);
                    Object rightValue = keysMap.get(key) != null ? right.get(keysMap.get(key)) : null;
                    if (!Objects.equals(leftValue, rightValue)) {
                        gpl.getFieldMap().put(key, leftValue != null ? leftValue.toString() : "null");
                        gpr.getFieldMap().put(keysMap.get(key), rightValue != null ? rightValue.toString() : "null");
                        /*throw new AssertionError(
                                String.format("Field mismatch for '%s': left=%s, right=%s, joinKey=%s",
                                        key, leftValue, rightValue, left.get("joinKey"))
                        );*/
                    }
                }
            }
            if((!gpl.getFieldMap().isEmpty() && gpl.getFieldMap().size()>0) || (!gpr.getFieldMap().isEmpty() && gpr.getFieldMap().size()>0) ){
                if (ReportsContext.genericPojoMapForFailureWithKey == null) {
                    ReportsContext.genericPojoMapForFailureWithKey = new HashMap<>();
                }
                ReportsContext.genericPojoMapForFailureWithKey.computeIfAbsent(scenario.getName(), k -> new java.util.HashMap<>())
                        .computeIfAbsent("for each matched record, all the source fields should match target fields", k -> new ArrayList<>())
                        .addAll(java.util.Arrays.asList(gpl, gpr));
                throw new AssertionError("step failed due to mismatch in field values");
            }
        }
        for (FieldMapping mapping : ctx.spec.mappings) {
            if (mapping.getLeftField() == null || mapping.getRightField().isEmpty()) {
                throw new IllegalArgumentException("Source field in mapping cannot be null or empty: " + mapping);
            }
            if (mapping.getRightField() == null || mapping.getRightField().isEmpty()) {
                throw new IllegalArgumentException("Target field in mapping cannot be null or empty: " + mapping);
            }
        }
    }

    private static double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof org.bson.types.Decimal128) return ((org.bson.types.Decimal128) value).doubleValue();
        return Double.parseDouble(value.toString());
    }

    @When("I query the collection")
    public void i_query_the_collection() {
        String[] parts = ctx.spec.sourceCollection.split("\\.", 2);
        String dbName = parts[0];
        String collName = parts[1];
        Document filter = ctx.spec.sourceFilter != null ? ctx.spec.sourceFilter : new Document();
        ctx.sourceDocs = ctx.clients.source
                .getDatabase(dbName)
                .getCollection(collName)
                .find(filter)
                .into(new java.util.ArrayList<>());
    }

    @Given("source collection is retrieved")
    public void source_collection_is_retrieved() {
        String[] parts = ctx.spec.sourceCollection.split("\\.", 2);
        String dbName = parts[0];
        String collName = parts[1];
        Document filter = ctx.spec.sourceFilter != null ? ctx.spec.sourceFilter : new Document();
        ctx.sourceDocs = ctx.clients.source
                .getDatabase(dbName)
                .getCollection(collName)
                .find(filter)
                .into(new java.util.ArrayList<>());
    }

    @Given("target collection is retrieved")
    public void target_collection_is_retrieved() {
        String[] parts = ctx.spec.targetCollection.split( "\\.", 2);
        String dbName = parts[0];
        String collName = parts[1];
        Document filter = ctx.spec.targetFilter != null ? ctx.spec.targetFilter : new Document();
        ctx.targetDocs = ctx.clients.target
                .getDatabase(dbName)
                .getCollection(collName)
                .find(filter)
                .into(new java.util.ArrayList<>());
    }

    // Java
    @When("I query both source and target collections")
    public void i_query_from_both_collections() {
        // Query source
        String[] srcParts = ctx.spec.sourceCollection.split( "\\.", 2);
        String srcDb = srcParts[0];
        String srcColl = srcParts[1];
        Document srcFilter = ctx.spec.sourceFilter != null ? ctx.spec.sourceFilter : new Document();
        ctx.sourceDocs = ctx.clients.source
                .getDatabase(srcDb)
                .getCollection(srcColl)
                .find(srcFilter)
                .into(new java.util.ArrayList<>());

        // Query target
        String[] tgtParts = ctx.spec.targetCollection.split( "\\.", 2);
        String tgtDb = tgtParts[0];
        String tgtColl = tgtParts[1];
        Document tgtFilter = ctx.spec.targetFilter != null ? ctx.spec.targetFilter : new Document();
        ctx.targetDocs = ctx.clients.target
                .getDatabase(tgtDb)
                .getCollection(tgtColl)
                .find(tgtFilter)
                .into(new java.util.ArrayList<>());

        // Set counts in result object
        if (ctx.result == null) {
            ctx.result = new ReconciliationResult();
        }
        ctx.result.sourceCount = ctx.sourceDocs.size();
        ctx.result.targetCount = ctx.targetDocs.size();
    }

    @When("I query source collection for count")
    public void querySourceCollectionForCount() {
        String[] parts = ctx.spec.sourceCollection.split( "\\.", 2);
        String dbName = parts[0];
        String collName = parts[1];
        Document filter = ctx.spec.sourceFilter != null ? ctx.spec.sourceFilter : new Document();
        long count = ctx.clients.source
                .getDatabase(dbName)
                .getCollection(collName)
                .countDocuments(filter);
        if (ctx.result == null) ctx.result = new ReconciliationResult();
        ctx.result.sourceCount = (int) count;
    }

    @When("I query target collection for count")
    public void queryTargetCollectionForCount() {
        String[] parts = ctx.spec.targetCollection.split( "\\.", 2);
        String dbName = parts[0];
        String collName = parts[1];
        Document filter = ctx.spec.targetFilter != null ? ctx.spec.targetFilter : new Document();
        long count = ctx.clients.target
                .getDatabase(dbName)
                .getCollection(collName)
                .countDocuments(filter);
        if (ctx.result == null) ctx.result = new ReconciliationResult();
        ctx.result.targetCount = (int) count;
    }

    @Then("record count of source collection should be {int}")
    public void recordCountofSourceCollectionShouldBe(int expectedCount) {
        Assertions.assertThat(ctx.sourceDocs.size())
                .isEqualTo(expectedCount);
    }

    @Then("total {string} of all records in source collection should be {double} within {double}")
    public void totalFieldOfAllRecordsShouldBeWithin(String field, double expectedTotal, double tolerance) {
        double sum = ctx.sourceDocs.stream()
                .mapToDouble(doc -> doc.get(field) != null ? ((Number) doc.get(field)).doubleValue() : 0.0)
                .sum();
        org.assertj.core.api.Assertions.assertThat(sum)
                .isCloseTo(expectedTotal, org.assertj.core.api.Assertions.offset(tolerance));
    }

    @Given("source records are decrypted on attribute {string}")
    public void source_records_are_decrypted_on_attribute(String attribute) {
        ctx.sourceDocs = decryptRecordsOnAttribute(ctx.sourceDocs, attribute);
    }

    @Given("target records are decrypted on attribute {string}")
    public void target_records_are_decrypted_on_attribute(String attribute) {
        ctx.targetDocs = decryptRecordsOnAttribute(ctx.targetDocs, attribute);
    }

    public List<Document> decryptRecordsOnAttribute(List<Document> docs, String attribute) {
        return docs.stream()
                .map(doc ->cryptService.decryptDocumentFields(doc.get(attribute).toString()))
                .collect(Collectors.toList());
    }

    @Given("I extract field {string} from decrypted source records")
    public void i_extract_field_from_decrypted_source_records(String fieldName) {
        if (ctx.extractedSourceValues == null) {
            ctx.extractedSourceValues = new HashMap<>();
        }
        ctx.extractedSourceValues.put(fieldName, extractField(ctx.sourceDocs, fieldName));
    }

    @Given("I extract field {string} from decrypted target records")
    public void i_extract_field_from_decrypted_target_records(String fieldName) {
        if (ctx.extractedTargetValues == null) {
            ctx.extractedTargetValues = new HashMap<>();
        }
        ctx.extractedTargetValues.put(fieldName, extractField(ctx.targetDocs, fieldName));
    }

    @Given("I encrypt field {string} from source records using {string} as crypt field")
    public void i_encrypt_field_from_source_records_using_as_crypt_field(String fieldName, String cryptField) {
        for (Document doc : ctx.sourceDocs) {
            Object value = doc;
            String[] keys = fieldName.split("\\.");
            // Traverse to the parent document of the target field
            for (int i = 0; i < keys.length - 1; i++) {
                if (value instanceof Document) {
                    value = ((Document) value).get(keys[i]);
                } else {
                    value = null;
                    break;
                }
            }
            if (value instanceof Document) {
                Document parent = (Document) value;
                String lastKey = keys[keys.length - 1];
                Object fieldValue = parent.get(lastKey);
                if (fieldValue != null) {
                    var result = cryptService.encryptField(fieldValue.toString(), cryptField);
                    if (result != null) {
                        parent.put(lastKey, result.encryptedValue());
                    }
                }
            }
        }
    }

    //And I join source field "chdBaseSegment.chdFullAcctNo.chdAccountNumber" to target field "reaAgentUuid" via joining collection "Optis_Dev.Accou
    @Given("I join source field {string} to target field {string} via joining collection {string} using joining fields {string} to {string}")
    public void i_join_source_field_to_target_field_via_joining_collection_using_joining_fields_to(
            String sourceField,
            String targetField,
            String joiningCollection,
            String joiningSourceField,
            String joiningTargetField) {

        Set<Object> sourceFieldValues = ctx.sourceDocs.stream()
                .map(doc -> extractField(Collections.singletonList(doc), sourceField))
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        String[] parts = joiningCollection.split( "\\.", 2);
        String dbName = parts[0];
        String collName = parts[1];

        Document filter = new Document(joiningSourceField, new Document("$in", sourceFieldValues));
        Document projection = new Document(joiningSourceField, 1).append(joiningTargetField, 1).append("_id", 0);
        List<Document> mappingDocs = ctx.clients.source
                .getDatabase(dbName)
                .getCollection(collName)
                .find(filter)
                .projection(projection)
                .into(new ArrayList<>());

        // Build a lookup map from joiningSourceField to joiningTargetField
        Map<Object, Object> mappingLookup = mappingDocs.stream()
                .filter(doc -> doc.get(joiningSourceField) != null && doc.get(joiningTargetField) != null)
                .collect(Collectors.toMap(
                        doc -> doc.get(joiningSourceField),
                        doc -> doc.get(joiningTargetField),
                        ( a, b) ->a // handle duplicates
                ));

        // Update each source document with the mapped value in the target field
        for (Document srcDoc : ctx.sourceDocs) {
            Object srcValue = extractField(Collections.singletonList(srcDoc), sourceField).stream().findFirst().orElse(null);
            if (srcValue != null && mappingLookup.containsKey(srcValue)) {
                srcDoc.put(targetField, mappingLookup.get(srcValue));
            }
        }

        // Optionally store mapping for later use
        ctx.spec.mappingCollection = joiningCollection;
        ctx.spec.mappingDocs = mappingDocs;

        // Set join keys in spec for reconciliation
        ctx.spec.joinKeys = List.of(
                JoinKey.of(
                        Map.of( "SourceField", targetField,"SourceFieldAs", targetField,"TargetField", targetField,"TargetFieldAs", targetField)
                )
        );
    }

    @When("I compare {string} value from source to {string} value from target")
    public void i_compare_value_from_source_to_value_from_target(String sourceField, String targetField) {
        if (ctx.result == null) ctx.result = new ReconciliationResult();

        List<Object> sourceList = Optional.ofNullable((List<Object>) ctx.extractedSourceValues.get(sourceField))
                .orElse(Collections.emptyList());
        List<Object> targetList = Optional.ofNullable((List<Object>) ctx.extractedTargetValues.get(targetField))
                .orElse(Collections.emptyList());

        Set<Object> sourceSet = new HashSet<>(sourceList);
        Set<Object> targetSet = new HashSet<>(targetList);

        int matchedCount = 0;
        for (Object value : sourceSet) {
            if (targetSet.contains(value)) {
                matchedCount++;
            } else {
                ctx.result.leftOnlyKeys.add(value.toString());
            }
        }
        ctx.result.sourceCount = sourceSet.size();
        ctx.result.targetCount = targetSet.size();
        ctx.result.matchedCount = matchedCount;
        ctx.result.matchCoveragePercent = (ctx.result.sourceCount == 0) ? 0.0 : ((double) matchedCount / ctx.result.sourceCount) * 100.0;
    }

    @Then("values from both source and target should match {double}%")
    public void values_from_both_source_and_target_should_match_percent(double expectedPercent) {
        if (ctx.result == null) {
            throw new AssertionError("Reconciliation result is missing.");
        }
        if (ctx.result.matchCoveragePercent < expectedPercent) {
            if (!ctx.result.leftOnlyKeys.isEmpty()) {
                System.out.print("Left only keys size   " + ctx.result.leftOnlyKeys.size());
                int count = ++counter;
                GenericPojo gpl = GenericPojo.builder().uniqueId(count).srcOrTar('S').collectionName(ctx.spec.sourceCollection).leftOnlyFields
                        (Map.of( "accountNumbers", new ArrayList<>(ctx.result.leftOnlyKeys))).build();
                GenericPojo gpr = GenericPojo.builder().uniqueId(count).srcOrTar('T').collectionName(ctx.spec.targetCollection).build();

                if (ReportsContext.genericPojoMapForFailureWithKey == null) {
                    ReportsContext.genericPojoMapForFailureWithKey = new HashMap<>();
                }

                ReportsContext.genericPojoMapForFailureWithKey
                        .computeIfAbsent(scenario.getName(), k -> new java.util.HashMap<>())
                        .computeIfAbsent( "values from both source and target should match " + expectedPercent + "%", k -> new java.util.ArrayList<>())
                        .addAll(java.util.Arrays.asList(gpl, gpr));
                System.out.println("GPL  " + gpl.toString() + "   GPR   " + gpr.toString());
            }
            throw new AssertionError("Match coverage percent is less than " + expectedPercent + "%. Actual: " + ctx.result.matchCoveragePercent);
        }
    }

    private boolean matchesFilter(Document doc, Document filter) {
        for (String key : filter.keySet()) {
            Object filterValue = filter.get(key);
            Object docValue = doc.get(key);
            if (docValue == null || !docValue.equals(filterValue)) {
                return false;
            }
        }
        return true;
    }

    public List<Object> extractField(List<Document> docs, String fieldPath) {
        List<Object> values = new ArrayList<>();
        for (Document doc : docs) {
            Object value = doc;
            for (String key : fieldPath.split( "\\.")) {
                if (value instanceof Document) {
                    value = ((Document) value).get(key);
                } else {
                    value = null;
                    break;
                }
            }
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private Object getNestedValue(Document doc, String fieldPath) {
        Object value = doc;
        for (String key : fieldPath.split("\\.")) {
            if (value instanceof Document) {
                value = ((Document) value).get(key);
            } else {
                return null;
            }
        }
        return value;
    }

    @Given("joining collection {string} is filtered by source field {string} and joining field {string} and retrieved")
    public void joining_collection_is_filtered_by_source_and_joining_field_and_retrieved(
            String joiningCollection, String sourceField, String joiningField) {
        // 1. Extract unique values from source documents
        Set<Object> sourceFieldValues = ctx.sourceDocs.stream()
                .map(doc -> extractField(Collections.singletonList(doc), sourceField))
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 2. Query the joining collection for documents where the relevant field matches any of these values
        String[] parts = joiningCollection.split("\\.",2);
        String dbName = parts[0];
        String collName = parts[1];

        Document filter = new Document(joiningField, new Document("$in", sourceFieldValues));
        List<Document> mappingDocs = ctx.clients.source
                .getDatabase(dbName)
                .getCollection(collName)
                .find(filter)
                .into(new ArrayList<>());

        ctx.spec.mappingCollection = joiningCollection;
        ctx.spec.mappingDocs = mappingDocs;
    }

    @Given("balance fields from source and target are listed in excel sheet {string}")
    public void balance_fields_from_source_and_target_are_listed_in_excel_sheet(String sheetName) {
        if (ctx.spec.configEntity == null || ctx.spec.configEntity.getBalanceFields() == null) {
            throw new IllegalStateException("ReconConfigEntity or its balance fields are not loaded.");
        }
        // Convert the balanceFields map to a list of maps for compatibility
        ctx.spec.balanceFieldMappings = ctx.spec.configEntity.getBalanceFields().entrySet().stream()
                .flatMap( entry -> entry.getValue().stream()
                        .map( field -> {
                            Map<String, String> map = new HashMap<>();
                            map.put("collection", entry.getKey());
                            map.put("fieldName", field);
                            return map;
                        })
                ).collect(Collectors.toList());
    }

    @When("I sum balance fields for both source and target")
    public void i_sum_balance_fields_for_both_source_and_target() {
        // Ensure balance field mappings are loaded
        if (ctx.spec.balanceFieldMappings == null || ctx.spec.balanceFieldMappings.isEmpty()) {
            throw new IllegalStateException("Balance field mappings are not defined.");
        }

        // Extract source and target balance field names
        // Extract source and target balance field names
        List<String> sourceFields = ctx.spec.balanceFieldMappings.stream()
                .filter( row -> "source".equals(row.get("collection")))
                .map( row -> row.get("fieldName"))
                .filter(Objects::nonNull)
                .toList();

        List<String> targetFields = ctx.spec.balanceFieldMappings.stream()
                .filter( row -> "target".equals(row.get("collection")))
                .map( row -> row.get("fieldName"))
                .filter(Objects::nonNull)
                .toList();

        // Run reconciliation to populate matchedPairs
        ReconciliationEngine engine = new ReconciliationEngine(ctx.clients.source, ctx.clients.target);
        ctx.result = engine.runBalanceCheck(
                ctx.sourceDocs,
                ctx.targetDocs,
                ctx.spec,
                sourceFields,
                targetFields,
                ctx.spec.defaultNumericTolerance != null ? ctx.spec.defaultNumericTolerance : BigDecimal.ZERO
        );

        // For each matched pair, sum the balance fields and store in the pair's documents
        for (com.fiserv.optis.qarecon.model.Pair<org.bson.Document, org.bson.Document> pair : ctx.result.matchedPairs) {
            double sourceSum = sourceFields.stream().mapToDouble(f->toDouble(pair.getLeft().get(f))).sum();

            double targetSum = targetFields.stream().mapToDouble(f-> toDouble(pair.getRight().get(f))).sum();

            pair.getLeft().put("_balanceSum",sourceSum);
            pair.getRight().put("_balanceSum",targetSum);
        }
    }

    // Add these methods to StepDefinitions.java

    @Given("balance field configurations from excel sheet {string}")
    public void balance_field_configurations_from_excel_sheet(String sheetName) {
        if (ctx.spec.configEntity == null || ctx.spec.configEntity.getBalanceFieldConfigs() == null) {
            throw new IllegalStateException("ReconConfigEntity or its balance field configs are not loaded.");
        }
        ctx.spec.balanceFieldConfigs = ctx.spec.configEntity.getBalanceFieldConfigs();
    }

    @When("I aggregate balance fields for both source and target with grouping and strategy")
    public void i_aggregate_balance_fields_with_grouping_and_strategy() {
        if (ctx.spec.balanceFieldConfigs == null || ctx.spec.balanceFieldConfigs.isEmpty()) {
            throw new IllegalStateException("Balance field configurations are not defined.");
        }

        // Separate source and target configurations
        List<BalanceFieldConfig> sourceConfigs = ctx.spec.balanceFieldConfigs.stream()
                .filter(config -> "source".equalsIgnoreCase(config.getCollection()))
                .collect(Collectors.toList());

        List<BalanceFieldConfig> targetConfigs = ctx.spec.balanceFieldConfigs.stream()
                .filter(config -> "target".equalsIgnoreCase(config.getCollection()))
                .collect(Collectors.toList());

        // Run enhanced reconciliation with grouping and aggregation
        ReconciliationEngine engine = new ReconciliationEngine(ctx.clients.source, ctx.clients.target);
        ctx.result = engine.runEnhancedBalanceCheck(
                ctx.sourceDocs,
                ctx.targetDocs,
                ctx.spec,
                sourceConfigs,
                targetConfigs,
                ctx.spec.defaultNumericTolerance != null ? ctx.spec.defaultNumericTolerance : BigDecimal.ZERO
        );
    }

    @Then("for each matched group, the aggregated balance from source should equal aggregated balance from target within {double} tolerance")
    public void for_each_matched_group_aggregated_balances_should_match_within_tolerance(Double tolerance) {
        if (ctx.result == null || ctx.result.fieldDiffs == null) {
            throw new IllegalStateException("Reconciliation result is missing.");
        }

        if (!ctx.result.fieldDiffs.isEmpty()) {
            // Build detailed error report
            int count = ++counter;

            List<BalanceFieldConfig> sourceConfigs = ctx.spec.balanceFieldConfigs.stream()
                    .filter(config -> "source".equalsIgnoreCase(config.getCollection()))
                    .collect(Collectors.toList());

            List<BalanceFieldConfig> targetConfigs = ctx.spec.balanceFieldConfigs.stream()
                    .filter(config -> "target".equalsIgnoreCase(config.getCollection()))
                    .collect(Collectors.toList());

            for (ReconciliationEngine.FieldDiff diff : ctx.result.fieldDiffs) {
                GenericPojo gpl = GenericPojo.builder()
                        .uniqueId(count)
                        .srcOrTar('S')
                        .collectionName(ctx.spec.sourceCollection)
                        .genericSingleFieldMap(Map.of(
                                "sourceAggregatedBalance", (BigDecimal) diff.leftValue,
                                "groupKey", diff.key
                        ))
                        .build();

                GenericPojo gpr = GenericPojo.builder()
                        .uniqueId(count)
                        .srcOrTar('T')
                        .collectionName(ctx.spec.targetCollection)
                        .genericSingleFieldMap(Map.of(
                                "targetAggregatedBalance", (BigDecimal) diff.rightValue,
                                "groupKey", diff.key,
                                "delta", (BigDecimal) diff.delta
                        ))
                        .build();

                if (ReportsContext.genericPojoMapForFailureWithKey == null) {
                    ReportsContext.genericPojoMapForFailureWithKey = new HashMap<>();
                }

                String stepName = String.format(
                        "for each matched group, the aggregated balance from source should equal aggregated balance from target within %s tolerance",
                        tolerance
                );

                ReportsContext.genericPojoMapForFailureWithKey
                        .computeIfAbsent(scenario.getName(), k -> new HashMap<>())
                        .computeIfAbsent(stepName, k -> new ArrayList<>())
                        .addAll(Arrays.asList(gpl, gpr));
            }

            throw new AssertionError(
                    String.format("Found %d balance mismatches. Check the report for details.",
                            ctx.result.fieldDiffs.size())
            );
        }
    }

    // Alternative step with more detailed configuration display
    @Then("aggregated balances should match with the following strategies:")
    public void aggregated_balances_should_match_with_strategies(DataTable table) {
        // This step allows inline verification of strategies used
        List<Map<String, String>> expectedStrategies = table.asMaps();

        // Validate that configurations match expectations
        for (Map<String, String> expected : expectedStrategies) {
            String collection = expected.get("collection");
            String field = expected.get("field");
            String strategy = expected.get("strategy");

            boolean found = ctx.spec.balanceFieldConfigs.stream()
                    .anyMatch(config ->
                            config.getCollection().equalsIgnoreCase(collection) &&
                                    config.getFieldName().equals(field) &&
                                    config.getAggregationStrategy().name().equalsIgnoreCase(strategy)
                    );

            if (!found) {
                throw new AssertionError(
                        String.format("Expected configuration not found: collection=%s, field=%s, strategy=%s",
                                collection, field, strategy)
                );
            }
        }

        // Then perform the actual balance check
        for_each_matched_group_aggregated_balances_should_match_within_tolerance(
                ctx.spec.defaultNumericTolerance.doubleValue()
        );
    }



    @Then("for each matched record, the sum of balance fields from source should be equal to sum balance fields on target within {double} tolerance")
    public void for_each_matched_record_the_total_of_source_balance_fields_should_equal_the_total_of_target_balance_fields_within_tolerance(Double tolerance) {
        // Get the list of source balance fields from the spec or context
        List<String> sourceFields = ctx.spec.balanceFieldMappings.stream()
                .filter( row -> "source".equals(row.get("collection")))
                .map( row -> row.get("fieldName"))
                .filter(Objects::nonNull)
                .toList();

        for (Pair<Document, Document> pair : ctx.result.matchedPairs) {
            // Sum source balance fields
            BigDecimal sourceSum = sourceFields.stream()
                    .map(  f -> {
                        Object v = getNestedValue(pair.getLeft(), f);
                        if (v == null) return BigDecimal.ZERO;
                        try { return new BigDecimal(v.toString()); } catch (Exception e) { return BigDecimal.ZERO; }
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal targetGroupedSum = pair.getRight().get("_groupedSum") != null
                    ? new BigDecimal(pair.getRight().get("_groupedSum").toString())
                    : BigDecimal.ZERO;

            if (sourceSum.subtract(targetGroupedSum).abs().compareTo(BigDecimal.valueOf(tolerance)) > 0) {
                int count = ++counter;
                GenericPojo gpl = GenericPojo.builder().uniqueId(count).srcOrTar('S').collectionName(ctx.spec.sourceCollection)
                        .genericSingleFieldMap(Map.of( "sourceBalanceSum", sourceSum)).build();
                GenericPojo gpr = GenericPojo.builder().uniqueId(count).srcOrTar('T').collectionName(ctx.spec.targetCollection)
                        .genericSingleFieldMap(Map.of( "targetBalanceSum", targetGroupedSum)).build();
                if (ReportsContext.genericPojoMapForFailureWithKey == null) {
                    ReportsContext.genericPojoMapForFailureWithKey = new HashMap<>();
                }

                ReportsContext.genericPojoMapForFailureWithKey.computeIfAbsent(scenario.getName(), k -> new java.util.HashMap<>())
                        .computeIfAbsent( "for each matched record, the sum of balance fields " +
                                "from source should be equal to sum balance fields on target within " + tolerance +
                                " tolerance", k -> new java.util.ArrayList<>())
                        .addAll(java.util.Arrays.asList(gpl, gpr));
                throw new AssertionError(
                        String.format("Balance sum mismatch: sourceSum=%s, targetSum=%s, joinKey=%s",
                                sourceSum, targetGroupedSum, pair.getLeft().get("joinKey"))
                );
            }
        }
    }
}
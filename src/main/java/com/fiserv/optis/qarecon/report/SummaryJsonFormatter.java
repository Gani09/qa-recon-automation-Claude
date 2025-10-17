package com.fiserv.optis.qarecon.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fiserv.optis.qarecon.constants.ReportsContext;
import com.fiserv.optis.qarecon.dbEntities.ReportEntity;
import com.fiserv.optis.qarecon.model.Detail;
import com.fiserv.optis.qarecon.report.model.CollectionTotalCount;
import com.fiserv.optis.qarecon.report.model.GenericPojo;
import com.fiserv.optis.qarecon.repository.ReportRepository;
import com.fiserv.optis.qarecon.runner.StepDefinitions;
import com.fiserv.optis.qarecon.service.MongoService;
import com.fiserv.optis.qarecon.service.ReportService;
import com.fiserv.optis.qarecon.util.SpringContextHolder;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestRunFinished;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SummaryJsonFormatter implements ConcurrentEventListener {
    private int total = 0, passed = 0, failed = 0;
    static int totalExecuted = 0;
    static int totalPassed = 0;
    static int totalFailed = 0;

    private ReportService reportSvc;

    public SummaryJsonFormatter() {
    }

    public SummaryJsonFormatter(ReportService reportSvc) {
        ApplicationContext ctx = SpringContextHolder.getApplicationContext();
        this.reportSvc = ctx.getBean(ReportService.class);
    }

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseFinished.class, this::handleTestCaseFinished);
        publisher.registerHandlerFor(TestRunFinished.class, this::handleTestRunFinished);
    }

    private void handleTestCaseFinished(TestCaseFinished event) {
        total++;
        String status = event.getResult().getStatus().name().toLowerCase();
        System.out.println("Status is " + status);
        if ("passed".equals(status)) passed++;
        else if ("failed".equals(status) || "undefined".equals(status)) failed++;
    }

    private void handleTestRunFinished(TestRunFinished event) {
        /*ObjectMapper mapper = new ObjectMapper();
        ObjectNode failureSummary = mapper.createObjectNode();*/
        Map<String, ArrayNode> dynamicRefs = new HashMap<>();
        //Map<String, List<GenericPojo>> failureMap = getGenericMap();
        totalExecuted = total;
        totalPassed = passed;
        totalFailed = failed;
        ObjectMapper mapperNew = new ObjectMapper();
        ObjectNode failureSummaryNew = mapperNew.createObjectNode();
        SummaryJsonFormatter formatter = new SummaryJsonFormatter(reportSvc);
        Map<String, Map<String, List<GenericPojo>>> failureMapWithKey = ReportsContext.genericPojoMapForFailureWithKey;
        // All Above are for level 1
        if (failureMapWithKey != null) {
            failureMapWithKey.entrySet().stream()
                    .forEach( level1Entry -> {
                //for (Map.Entry<String, Map<String, List<GenericPojo>>> level1Entry : failureMapWithKey.entrySet()) {
                ObjectNode level1i1 = mapperNew.createObjectNode();
                String level1Key = level1Entry.getKey();
                Map<String, List<GenericPojo>> level2Map = level1Entry.getValue();
                for (Map.Entry<String, List<GenericPojo>> level2Entry : level2Map.entrySet()) {
                    ObjectNode level122 = mapperNew.createObjectNode();
                    ArrayNode arrayNode22 = mapperNew.createArrayNode();
                    String level2Key = level2Entry.getKey();
                    Map<Integer, List<GenericPojo>> uniqueIdMap = new HashMap<>();
                    uniqueIdMap = level2Entry.getValue().stream().collect(Collectors.groupingBy(GenericPojo::getUniqueId));

                    for (Map.Entry<Integer, List<GenericPojo>> uniqueIdEntry : uniqueIdMap.entrySet()) {
                        ObjectNode level2Intermediate = mapperNew.createObjectNode();
                        ObjectNode level2 = mapperNew.createObjectNode();

                        //Map<String, ArrayNode> dynamicRefs2 = new HashMap<>(); // level 3
                        ObjectNode node1 = mapperNew.createObjectNode();
                        ArrayNode arrayNode1 = mapperNew.createArrayNode();
                        for (GenericPojo gp : uniqueIdEntry.getValue()) {
                            //boolean onLevel2.put("sourceCollectionName", gp.getCollectionName());tyLeftFlag = false;
                            level2.put( "status", "failed");
                            switch (gp.getSrcOrTar()) {
                                case 'S' -> {
                                    if (gp.getCollectionName() != null) {
                                        level2.put( "sourceCollectionName", gp.getCollectionName());
                                    }
                                    if (!gp.getLeftOnlyFields().isEmpty()) {
                                        if (gp.getLeftOnlyFields().get("accountNumbers") != null) {
                                            level2.put( "missing accounts in target", gp.getLeftOnlyFields().get("accountNumbers"));
                                        }
                                    }
                                    if (gp.getTotalCount() != null) {
                                        level2.put("sourceTotalCount", gp.getTotalCount());
                                    }
                                    Map<String, ArrayNode> dynamicRefsSrcFields = new HashMap<>(); // Level 2
                                    if (gp.getFieldMap() != null && gp.getFieldMap().size() > 0) {
                                        dynamicRefsSrcFields.put("srcFields", mapperNew.createArrayNode());
                                        for (Map.Entry<String, Object> fieldEntry : gp.getFieldMap().entrySet()) {
                                            String fieldName = fieldEntry.getKey();
                                            Object fieldValue = fieldEntry.getValue();
                                            ObjectNode subnode1 = mapperNew.createObjectNode();
                                            subnode1.put(fieldName, fieldValue != null ? fieldValue.toString() : "null");
                                            dynamicRefsSrcFields.get("srcFields").add(subnode1);
                                        }
                                    }
                                    level2.set("srcFields", dynamicRefsSrcFields.get("srcFields"));
                                }
                                if (gp.getGenericSingleFieldMap() != null && gp.getGenericSingleFieldMap().size() > 0) {
                                    for (Map.Entry<String, Object> fieldEntry : gp.getGenericSingleFieldMap().entrySet()) {
                                        String fieldName = fieldEntry.getKey();
                                        BigDecimal fieldValue = (BigDecimal) fieldEntry.getValue();
                                        level2.put(fieldName, fieldValue);
                                    }
                                }
                            }
                            case 'T' -> {
                                if (gp.getCollectionName() != null) {
                                    level2.put( "targetCollectionName", gp.getCollectionName());
                                }
                                if (gp.getTotalCount() != null) {
                                    level2.put( "targetTotalCount", gp.getTotalCount());
                                }
                                if (gp.getFieldMap() != null && gp.getFieldMap().size() > 0) {
                                    Map<String, ArrayNode> dynamicRefsTargetFields = new HashMap<>(); // Level 2
                                    dynamicRefsTargetFields.put("targetFields", mapperNew.createArrayNode());
                                    for (Map.Entry<String, Object> fieldEntry : gp.getFieldMap().entrySet()) {
                                        String fieldName = fieldEntry.getKey();
                                        Object fieldValue = fieldEntry.getValue();
                                        ObjectNode subnode1 = mapperNew.createObjectNode();
                                        subnode1.put(fieldName, fieldValue != null ? fieldValue.toString() : "null");
                                        dynamicRefsTargetFields.get("targetFields").add(subnode1);
                                    }
                                    level2.set("targetFields", dynamicRefsTargetFields.get("targetFields"));
                                }
                                if (gp.getGenericSingleFieldMap() != null && gp.getGenericSingleFieldMap().size() > 0) {
                                    for (Map.Entry<String, Object> fieldEntry : gp.getGenericSingleFieldMap().entrySet()) {
                                        String fieldName = fieldEntry.getKey();
                                        BigDecimal fieldValue = (BigDecimal) fieldEntry.getValue();
                                        level2.put(fieldName, fieldValue);
                                    }
                                }
                            }
                        }
                    }
                    //arrayNode1.add(level2);
                    level122.setAll(level2);
                    //failureSummaryNew.set(entry.getKey(), level1);
                }
            });
        }
        ReportsContext.genericPojoMapForFailureWithKey = null;
        //writing to file
        try (FileWriter writer = new FileWriter( "target/cucumber-summary.txt")) {
            writer.write( "Total test cases executed: " + total + "\n");
            writer.write( "Total test cases passed: " + passed + "\n");

            writer.write( "Below is the list of data for passed test cases");
            writer.write( "\n");
            //     writer.write(mapper1.writerWithDefaultPrettyPrinter().writeValueAsString(successSummary));
            writer.write( "\n");

            writer.write( "Total test cases failed: " + failed + "\n");
            writer.write( "Below is the list of data for failed test cases");
            System.out.println("failureSummaryNew ———→   " + failureSummaryNew);
            writer.write(mapperNew.writerWithDefaultPrettyPrinter().writeValueAsString(failureSummaryNew));
            //mapperNew.writerWithDefaultPrettyPrinter().writeValue(writer, failureSummaryNew);
            String jsonString = mapperNew.writerWithDefaultPrettyPrinter().writeValueAsString(failureSummaryNew);
            Document doc = Document.parse(jsonString);
            reportSvc = formatter.reportSvc;
            formatter.writeToDB(failureSummaryNew, reportSvc);
            //mongoService.writeCollection("reconciliation_report", doc);
            writer.write( "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Clear the static maps after writing the report
        ReportsContext.genericPojoMap = null;
        ReportsContext.genericPojoMapForSuccessWithKey = null;
        ReportsContext.genericPojoMapForFailureWithKey = null;
        StepDefinitions.counter = 0;
    }

    //saving to db
    private void writeToDB(ObjectNode failureSummaryNew, ReportService reportSvc) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        List<Document> docs = new ArrayList<>();

        ReportEntity entity = new ReportEntity();
        List<Detail> details = new ArrayList<>();
        for (Iterator<String> it = failureSummaryNew.fieldNames(); it.hasNext(); ) {
            String scenarioKey = it.next();
            Detail detail = new Detail();
            ObjectNode scenarioNode = (ObjectNode) failureSummaryNew.get(scenarioKey);
            String scenarioJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(scenarioNode);
            detail.setScenario(scenarioKey);
            Document doc = Document.parse(scenarioJson);
            detail.setDoc(doc);
            details.add(detail);
        }

        entity.setReportId(ReportsContext.runId);
        entity.setTotalExecuted(totalExecuted);
        entity.setTotalPassed(totalPassed);
        entity.setTotalFailed(totalFailed);
        entity.setDetails(details);
        reportSvc.saveReport(entity);
    }
}
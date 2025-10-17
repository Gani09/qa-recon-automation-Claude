package com.fiserv.optis.qarecon.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StepCaptureFormatter implements ConcurrentEventListener {

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<ObjectNode> capturedSteps = new ArrayList<>();

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestStepFinished.class, this::handleStepFinished);
        publisher.registerHandlerFor(TestRunFinished.class, this::handleRunFinished);
    }

    private void handleStepFinished(TestStepFinished event) {
        TestStep step = event.getTestStep();
        if (!(step instanceof PickleStepTestStep)) return;

        PickleStepTestStep pickleStep = (PickleStepTestStep) step;
        String keyword = pickleStep.getStep().getKeyword().trim();

        if (!keyword.equals("When") && !keyword.equals("Then")) return;

        ObjectNode node = mapper.createObjectNode();
        node.put("keyword", keyword);
        node.put("text", pickleStep.getStep().getText());
        node.put("status", event.getResult().getStatus().name().toLowerCase());
        capturedSteps.add(node);
    }

    private void handleRunFinished(TestRunFinished event) {
        ArrayNode arr = mapper.createArrayNode();
        arr.addAll(capturedSteps);

        try (FileWriter writer = new FileWriter("target/step-capture.json")) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, arr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
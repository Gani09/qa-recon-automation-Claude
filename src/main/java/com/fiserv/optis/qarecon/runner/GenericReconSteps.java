package com.fiserv.optis.qarecon.runner;

import com.fiserv.optis.qarecon.recon.CsvFileClient;
import com.fiserv.optis.qarecon.recon.ReconciliationReport;
import com.fiserv.optis.qarecon.recon.SimpleKeyReconciler;
import com.fiserv.optis.qarecon.util.ExcelUtils;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GenericReconSteps {

    private List<Map<String, Object>> left;
    private List<Map<String, Object>> right;
    private ReconciliationReport report;

    @Given("left csv {string}")
    public void left_csv(String path) throws Exception {
        left = new CsvFileClient().fetch(path);
    }

    @Given("right csv {string}")
    public void right_csv(String path) throws Exception {
        right = new CsvFileClient().fetch(path);
    }

    @When("reconcile on key {string}")
    public void reconcile_on_key(String key) {
        report = new SimpleKeyReconciler().reconcile(left, right, key);
    }

    @Then("matched should equal {int}")
    public void matched_should_equal(Integer expected) {
        if (report.matched != expected) {
            throw new AssertionError("Matched=" + report.matched + " expected=" + expected);
        }
        System.out.println(report.toText());
    }
}
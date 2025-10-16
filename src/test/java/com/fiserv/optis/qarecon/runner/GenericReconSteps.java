package com.fiserv.optis.qarecon.runner;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

public class GenericReconSteps {
    @Given("a trivial precondition") public void pre(){}
    @When("a trivial action happens") public void act(){}
    @Then("a trivial assertion holds") public void assertOk(){}
}

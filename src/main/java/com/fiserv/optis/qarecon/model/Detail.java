package com.fiserv.optis.qarecon.model;

import org.bson.Document;

public class Detail {

    private String scenario;
    private org.bson.Document doc;

    // Default constructor
    public Detail() {}

    // Full constructor
    public Detail(String scenario, Document doc) {
        this.scenario = scenario;
        this.doc = doc;
    }

    // Getters and setters
    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public org.bson.Document getDoc() {
        return doc;
    }

    public void setDoc(org.bson.Document doc) {
        this.doc = doc;
    }

    @Override
    public String toString() {
        return "Detail{" +
                "scenario='" + scenario + '\'' +
                ", docs=" + doc +
                '}';
    }
}
package com.fiserv.optis.qarecon.model;

import org.bson.Document;

public class Detail {
    private String scenario;
    private Document doc;

    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
    public Document getDoc() { return doc; }
    public void setDoc(Document doc) { this.doc = doc; }

    @Override public String toString(){
        return "Detail{scenario='%s', doc=%s}".formatted(scenario, doc==null?null:doc.toJson());
    }
}

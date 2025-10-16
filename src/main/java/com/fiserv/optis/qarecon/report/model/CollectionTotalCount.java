package com.fiserv.optis.qarecon.report.model;
public class CollectionTotalCount {
    private String collection;
    private int total;
    public CollectionTotalCount(){}
    public CollectionTotalCount(String c, int t){ this.collection=c; this.total=t; }
    public String getCollection(){ return collection; }
    public void setCollection(String c){ this.collection=c; }
    public int getTotal(){ return total; }
    public void setTotal(int t){ this.total=t; }
}

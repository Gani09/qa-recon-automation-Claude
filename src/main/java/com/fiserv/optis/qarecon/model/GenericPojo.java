package com.fiserv.optis.qarecon.model;

import java.util.HashMap;
import java.util.Map;

public class GenericPojo {
    private int uniqueId;
    private char srcOrTar;
    private String collectionName;
    private Integer totalCount;
    private Map<String, Object> leftOnlyFields = new HashMap<>();

    public int getUniqueId() { return uniqueId; }
    public void setUniqueId(int uniqueId) { this.uniqueId = uniqueId; }
    public char getSrcOrTar() { return srcOrTar; }
    public void setSrcOrTar(char srcOrTar) { this.srcOrTar = srcOrTar; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public Map<String, Object> getLeftOnlyFields() { return leftOnlyFields; }
    public void setLeftOnlyFields(Map<String, Object> leftOnlyFields) { this.leftOnlyFields = leftOnlyFields; }
}

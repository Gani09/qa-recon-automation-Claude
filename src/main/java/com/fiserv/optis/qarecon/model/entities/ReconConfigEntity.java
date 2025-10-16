package com.fiserv.optis.qarecon.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fiserv.optis.qarecon.model.FieldMapping;
import com.fiserv.optis.qarecon.model.JoinKey;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "QA_Automation_Configs")
public class ReconConfigEntity {
    @Id private String configId;
    @Indexed(unique = true) private String configName;
    private Map<String, Object> filters;
    private List<JoinKey> joinKeys;
    private List<FieldMapping> fieldMappings;
    private Map<String, java.util.List<String>> balanceFields;

    public String getConfigId(){ return configId; }
    public void setConfigId(String id){ this.configId=id; }
    public String getConfigName(){ return configName; }
    public void setConfigName(String n){ this.configName=n; }
    public Map<String, Object> getFilters(){ return filters; }
    public void setFilters(Map<String, Object> f){ this.filters=f; }
    public List<JoinKey> getJoinKeys(){ return joinKeys; }
    public void setJoinKeys(List<JoinKey> k){ this.joinKeys=k; }
    public List<FieldMapping> getFieldMappings(){ return fieldMappings; }
    public void setFieldMappings(List<FieldMapping> m){ this.fieldMappings=m; }
    public Map<String, java.util.List<String>> getBalanceFields(){ return balanceFields; }
    public void setBalanceFields(Map<String, java.util.List<String>> bf){ this.balanceFields=bf; }
}

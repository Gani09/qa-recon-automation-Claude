package com.fiserv.optis.qarecon.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fiserv.optis.qarecon.model.FieldMapping;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fiserv.optis.qarecon.model.JoinKey;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "QA_Automation_Configs")
public class ReconConfigEntity {

    @Id
    private String configId;

    @Indexed(unique = true)
    private String configName;

    private Map<String, Object> filters;
    private List<JoinKey> joinKeys;
    private List<FieldMapping> fieldMappings;
    private Map<String, List<String>> balanceFields;

    // Getters and setters
    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    public List<JoinKey> getJoinKeys() {
        return joinKeys;
    }

    public void setJoinKeys(List<JoinKey> joinKeys) {
        this.joinKeys = joinKeys;
    }

    public List<FieldMapping> getFieldMappings() {
        return fieldMappings;
    }

    public void setFieldMappings(List<FieldMapping> fieldMappings) {
        this.fieldMappings = fieldMappings;
    }

    public Map<String, List<String>> getBalanceFields() {
        return balanceFields;
    }

    public void setBalanceFields(Map<String, List<String>> balanceFields) {
        this.balanceFields = balanceFields;
    }
}
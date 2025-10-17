package com.fiserv.optis.qarecon.report.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@Builder
public class GenericPojo {

    private Integer uniqueId;
    private String collectionName;
    private Character srcOrTar;
    private Integer totalCount;
    private String prKey;

    @Builder.Default
    private Map<String, Object> fieldMap = new HashMap<>();

    @Builder.Default
    private Map<String, List<String>> lefOnlyFields = new HashMap<>();

    @Builder.Default
    private Map<String, List<String>> rightOnlyFields = new HashMap<>();

    private String then;

    @Builder.Default
    private Map<String, Object> genericSingleFieldMap = new HashMap<>();


}
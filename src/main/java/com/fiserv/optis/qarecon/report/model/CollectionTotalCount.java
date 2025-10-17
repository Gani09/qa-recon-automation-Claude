package com.fiserv.optis.qarecon.report.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CollectionTotalCount {

    private int sourceTotalCount;
    private int targetTotalCount;
    private String sourceCollectionName;
    private String targetCollectionName;
}
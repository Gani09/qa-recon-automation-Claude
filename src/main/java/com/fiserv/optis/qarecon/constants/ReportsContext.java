package com.fiserv.optis.qarecon.constants;

import com.fiserv.optis.qarecon.report.model.CollectionTotalCount;
import com.fiserv.optis.qarecon.report.model.GenericPojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ReportsContext {

    public static List<CollectionTotalCount> failedTotalCountList = new ArrayList<>();

    public static List<Object> successTotalCountList = new ArrayList<>();

    public static Map<String, List<Object>> failureMap = new ConcurrentHashMap<>();

    public static Map<String, List<Object>> successMap = new ConcurrentHashMap<>();

    public static Map<String, AtomicInteger> failureCountMap = new ConcurrentHashMap<>();

    public static Map<String, List<GenericPojo>> genericPojoMap = new ConcurrentHashMap<>();

    public static Map<String, List<GenericPojo>> genericPojoMapForSuccess = new ConcurrentHashMap<>();

    public static Map<String, Map<String, List<GenericPojo>>> genericPojoMapForSuccessWithKey = new ConcurrentHashMap<>();

    public static Map<String, Map<String, List<GenericPojo>>> genericPojoMapForFailureWithKey = new ConcurrentHashMap<>();

    public static String runId = null;
}
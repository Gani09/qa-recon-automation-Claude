package com.fiserv.optis.qarecon.recon;

import java.util.ArrayList;
import java.util.List;

public class ReconciliationReport {

    public int leftCount;
    public int rightCount;
    public int matched;
    public int mismatched;
    public int leftOnly;
    public int rightOnly;
    public List<String> sampleIssues = new ArrayList<>();

    public String toText() {
        return String.format(
                "Left: %d, Right: %d, Matched: %d, Mismatched: %d, LeftOnly: %d, RightOnly: %d\nIssues: %s",
                leftCount, rightCount, matched, mismatched, leftOnly, rightOnly, sampleIssues);
    }
}
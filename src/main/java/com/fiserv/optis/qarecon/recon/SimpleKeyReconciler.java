package com.fiserv.optis.qarecon.recon;

import java.util.*;

public class SimpleKeyReconciler implements Reconciler {

    @Override
    public ReconciliationReport reconcile(List<Map<String, Object>> left,
                                          List<Map<String, Object>> right,
                                          String keyField) {
        ReconciliationReport r = new ReconciliationReport();
        r.leftCount = left.size();
        r.rightCount = right.size();

        Map<Object, Map<String, Object>> rightIdx = new HashMap<>();
        for (var row : right) {
            rightIdx.put(row.get(keyField), row);
        }

        for (var lrow : left) {
            Object k = lrow.get(keyField);
            Map<String, Object> rrow = rightIdx.remove(k);

            if (rrow == null) {
                r.leftOnly++;
                continue;
            }

            if (lrow.equals(rrow)) {
                r.matched++;
            } else {
                r.mismatched++;
                r.sampleIssues.add("Key " + k + " differs: L=" + lrow + " R=" + rrow);
            }
        }

        r.rightOnly = rightIdx.size();
        return r;
    }
}
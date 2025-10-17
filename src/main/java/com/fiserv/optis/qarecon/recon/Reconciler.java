package com.fiserv.optis.qarecon.recon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface Reconciler {

    ReconciliationReport reconcile(List<Map<String, Object>> left,
                                   List<Map<String, Object>> right,
                                   String keyField);
}
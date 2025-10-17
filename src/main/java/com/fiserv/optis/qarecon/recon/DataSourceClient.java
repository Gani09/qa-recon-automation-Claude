package com.fiserv.optis.qarecon.recon;

import java.util.*;

public interface DataSourceClient {

    List<Map<String, Object>> fetch(String queryOrSpec) throws Exception;
}
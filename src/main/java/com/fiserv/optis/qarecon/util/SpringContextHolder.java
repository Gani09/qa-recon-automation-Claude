package com.fiserv.optis.qarecon.util;

import org.springframework.context.ApplicationContext;

// Utility class to hold the Spring ApplicationContext
public class SpringContextHolder {
    private static ApplicationContext context;

    public static void setApplicationContext(ApplicationContext ctx) {
        context = ctx;
    }

    public static ApplicationContext getApplicationContext() {
        return context;
    }
}
package com.fiserv.optis.qarecon.util;

import org.springframework.context.ApplicationContext;

public class SpringContextHolder {
    private static ApplicationContext context;
    public static void setApplicationContext(ApplicationContext ctx) { context = ctx; }
    public static ApplicationContext getApplicationContext() { return context; }
    public static <T> T getBean(Class<T> type) {
        if (context == null) throw new IllegalStateException("ApplicationContext not initialized");
        return context.getBean(type);
    }
}

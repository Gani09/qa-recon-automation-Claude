package com.fiserv.optis.qarecon;

import com.fiserv.optis.qarecon.util.SpringContextHolder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class QaReconApplication {
    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(QaReconApplication.class, args);
        SpringContextHolder.setApplicationContext(ctx);
    }
}

package com.yun.methodretry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 *
 *
 * @author raoliwen
 * @date 2026/01/12
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MethodRetryApplication {

    public static void main(String[] args) {
        SpringApplication.run(MethodRetryApplication.class, args);
    }

}

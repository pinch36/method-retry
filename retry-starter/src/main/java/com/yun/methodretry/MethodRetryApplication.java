package com.yun.methodretry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MethodRetryApplication {

    public static void main(String[] args) {
        SpringApplication.run(MethodRetryApplication.class, args);
    }

}

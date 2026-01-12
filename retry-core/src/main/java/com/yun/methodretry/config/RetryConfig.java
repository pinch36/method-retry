package com.yun.methodretry.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 *
 * @author raoliwen
 * @date 2026/01/12
 */
@Data
@ConfigurationProperties(prefix = "spring.retry")
public class RetryConfig {
    private Long interval = 5000000L;
    private boolean autoRetry = false;
}

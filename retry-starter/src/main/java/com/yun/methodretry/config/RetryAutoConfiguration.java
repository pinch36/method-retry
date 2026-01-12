package com.yun.methodretry.config;

import com.yun.methodretry.aspect.RetryAspect;
import com.yun.methodretry.retry.RetryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 *
 * @author raoliwen
 * @date 2026/01/12
 */
@Configuration
@EnableConfigurationProperties(RetryConfig.class)
public class RetryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RetryService retryService() {
        return new RetryService();
    }

    @Bean
    @ConditionalOnMissingBean
    public RetryAspect retryAspect() {
        return new RetryAspect();
    }

    @Bean
    @ConditionalOnMissingBean
    public RetrySchedulerConfig retrySchedulerConfig(){
        return new RetrySchedulerConfig();
    }
}

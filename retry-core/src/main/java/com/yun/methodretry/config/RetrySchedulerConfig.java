package com.yun.methodretry.config;

import com.yun.methodretry.retry.RetryService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import javax.annotation.Resource;

/**
 *
 *
 * @author raoliwen
 * @date 2026/01/12
 */
@Configuration
@EnableScheduling
public class RetrySchedulerConfig implements SchedulingConfigurer {

    @Resource
    private RetryConfig retryConfig;

    @Resource
    private RetryService retryService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        if (!retryConfig.isAutoRetry()) {
            return; // 不注册任务
        }

        // 按 interval（毫秒）固定频率执行
        taskRegistrar.addFixedRateTask(retryService::retryJob, retryConfig.getInterval());
    }
}

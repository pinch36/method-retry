package com.yun.methodretry.utils;

import lombok.extern.slf4j.Slf4j;

/**
 *
 *
 * @author raoliwen
 * @date 2025/12/31
 */
@Slf4j
public class RetryUtils {
    public static void retry(RetryFunction retryFunction, int maxTryTime, int initSleepMillis) throws Exception {
        for(int retryCnt = 1; retryCnt <= maxTryTime; ++retryCnt) {
            try {
                retryFunction.handle();
                break;
            } catch (Throwable ex) {
                if (retryCnt == maxTryTime) {
                    log.error("RetryUtil occur some ex {},do max {} retry but still fail.", ex, retryCnt);
                    throw new RuntimeException(ex);
                }

                System.out.println("RetryUtil occur some ex {},and we will retry {}.");
                log.error("RetryUtil occur some ex {},and we will retry {}.", ex, retryCnt);
            }
        }
    }
    public interface RetryFunction {
        void handle() throws Throwable;
    }
}

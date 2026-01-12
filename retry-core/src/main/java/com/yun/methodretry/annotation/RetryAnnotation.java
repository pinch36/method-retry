package com.yun.methodretry.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 *
 *
 * @author raoliwen
 * @date 2025/12/29
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RetryAnnotation {
    int retryTime() default 0;
    // 默认单位分钟
    int startTime() default 0;
    String beanName() default "";
}

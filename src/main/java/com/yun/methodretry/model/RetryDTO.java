package com.yun.methodretry.model;

import com.yun.methodretry.utils.SelfMethodUtils;
import lombok.Data;

/**
 *
 *
 * @author raoliwen
 * @date 2025/12/31
 */
@Data
public class RetryDTO {
    int retryTime;
    int startTime;
    SelfMethodUtils.MethodCallSpec methodCallSpec;
}

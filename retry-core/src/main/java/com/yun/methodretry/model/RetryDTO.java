package com.yun.methodretry.model;

import com.yun.methodretry.utils.SelfMethodUtils;
import lombok.Data;
import lombok.Getter;

/**
 *
 *
 * @author raoliwen
 * @date 2025/12/31
 */
@Data
public class RetryDTO {
    long id;
    int retryTime;
    SelfMethodUtils.MethodCallSpec methodCallSpec;
    RetryStatus retryStatus;

    @Getter
    public enum RetryStatus {
        SUCCESS(0, "成功"),
        FAIL(1, "失败"),
        NOT_YET_TIME(2, "未到处理时间");

        private final int code;
        private final String desc;

        RetryStatus(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }
}

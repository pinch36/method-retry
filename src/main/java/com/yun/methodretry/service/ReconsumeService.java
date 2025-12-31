package com.yun.methodretry.service;

import com.yun.methodretry.model.RetryDTO;

/**
 *
 *
 * @author raoliwen
 * @date 2025/12/31
 */
public interface ReconsumeService {
    void writeDB(RetryDTO retryDTO);
}

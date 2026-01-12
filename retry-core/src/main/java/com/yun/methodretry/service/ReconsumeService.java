package com.yun.methodretry.service;

import com.yun.methodretry.model.RetryDTO;

import java.util.List;

/**
 *
 *
 * @author raoliwen
 * @date 2025/12/31
 */
public interface ReconsumeService {
    void writeDB(List<RetryDTO> retryDTOs);
    List<RetryDTO> readDB(Object... objects);
    void updateDB(List<RetryDTO> retryDTOs);
}

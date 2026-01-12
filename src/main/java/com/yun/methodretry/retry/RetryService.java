package com.yun.methodretry.retry;

import com.yun.methodretry.config.RetryConfig;
import com.yun.methodretry.model.RetryDTO;
import com.yun.methodretry.service.ReconsumeService;
import com.yun.methodretry.utils.SelfMethodUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 *
 *
 * @author raoliwen
 * @date 2026/01/12
 */
@Slf4j
@Component
public class RetryService {
    @Resource
    private ReconsumeService reconsumeService;
    @Resource
    private ApplicationContext applicationContext;

    public void retryJob(){
        List<RetryDTO> retryDTOs = reconsumeService.readDB();
        retryDTOs = retry(retryDTOs);
        reconsumeService.updateDB(retryDTOs);
    }

    public List<RetryDTO> retry(List<RetryDTO> retryDTOs){
        for (RetryDTO retryDTO : retryDTOs) {
            try {
                SelfMethodUtils.MethodCallSpec methodCallSpec = retryDTO.getMethodCallSpec();
                if (Objects.isNull(methodCallSpec)) {
                    throw new IllegalArgumentException("methodCallSpec is null");
                }
                LocalDateTime startTime = methodCallSpec.getStartTime();
                if (Objects.isNull(startTime)) {
                    throw new IllegalArgumentException("startTime is null");
                }
                if (startTime.isAfter(LocalDateTime.now())) {
                    retryDTO.setRetryStatus(RetryDTO.RetryStatus.NOT_YET_TIME);
                    continue;
                }
                Object o = SelfMethodUtils.invokeFromSpec(applicationContext.getBean(methodCallSpec.getBeanName()),methodCallSpec);
                retryDTO.setRetryStatus(RetryDTO.RetryStatus.SUCCESS);
            }catch (Exception e){
                log.error("RPCReconsumeService handleMqMsg error:", e);
                retryDTO.setRetryStatus(RetryDTO.RetryStatus.FAIL);
            }
        }
        return retryDTOs;
    }
}

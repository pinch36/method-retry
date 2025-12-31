package com.yun.methodretry.aspect;

import com.yun.methodretry.annotation.RetryAnnotation;
import com.yun.methodretry.model.RetryDTO;
import com.yun.methodretry.service.ReconsumeService;
import com.yun.methodretry.utils.RetryUtils;
import com.yun.methodretry.utils.SelfMethodUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 *
 * @author raoliwen
 * @date 2025/12/29
 */
@Aspect
@Component
@Slf4j
public class RetryAspect {
    @Resource
    ReconsumeService  reconsumeService;

    @Around("rpcMethodPointcut()")
    public Object rpcRetry(ProceedingJoinPoint pjp) throws Throwable {
        AtomicReference<Object> res = new AtomicReference<>(null);
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Object target = pjp.getTarget();
        if(Objects.isNull(signature) || Objects.isNull(target)){
            log.info("rpcRetry has null");
            return pjp.proceed();
        }
        Class<?> targetClass = AopUtils.getTargetClass(target);
        Method method = signature.getMethod();
        if(Objects.isNull(method)){
            log.info("rpcRetry has null");
            return pjp.proceed();
        }
        Method targetMethod = targetClass.getMethod(method.getName(), method.getParameterTypes());
        RetryAnnotation retryAnnotation = AnnotationUtils.findAnnotation(targetMethod, RetryAnnotation.class);
        if (retryAnnotation == null) {
            return pjp.proceed();
        }
        int retryTime = retryAnnotation.retryTime();
        int startTime = retryAnnotation.startTime();
        String beanName = retryAnnotation.beanName();
        AtomicReference<Throwable> e = new AtomicReference<>();
        try {
//            res.set(pjp.proceed());
            // test
            throw new RuntimeException("hello");
            // test
        }catch (Throwable throwable){
            SelfMethodUtils.MethodCallSpec methodCallSpec = SelfMethodUtils.buildMethodCallSpec(beanName,targetMethod,pjp.getArgs());
            if (SelfMethodUtils.cheek(methodCallSpec)) {
                SelfMethodUtils.removeProxyMark(methodCallSpec);
                throw new RuntimeException("rpcRetry failed");
            }
            RetryUtils.retry(()->{
                try {
//                    res.set(pjp.proceed());
//                     test
                    throw new RuntimeException("hello");
//                     test
                } catch (Throwable ex) {
                    log.warn("rpcRetry has exception", ex);
                    e.set(ex);
                }
            },retryTime,0);
            // 重试失败落库
            if (Objects.isNull(res.get())) {
                LocalDateTime now = null;
                if (startTime > 0) {
                    now = LocalDateTime.now().plusMinutes(startTime);
                }
                RetryDTO retryDTO = getRetryDTO(retryTime, startTime, methodCallSpec);
                reconsumeService.writeDB(retryDTO);
                throw new RuntimeException("rpcRetry failed");
            }
        }
        return res.get();
    }

    private static RetryDTO getRetryDTO(int retryTime, int startTime, SelfMethodUtils.MethodCallSpec methodCallSpec) {
        RetryDTO retryDTO = new RetryDTO();
        retryDTO.setRetryTime(retryTime);
        retryDTO.setStartTime(startTime);
        retryDTO.setMethodCallSpec(methodCallSpec);
        return retryDTO;
    }

    @Pointcut("@annotation(com.yun.methodretry.annotation.RetryAnnotation)")
    protected void rpcMethodPointcut() {
    }
}

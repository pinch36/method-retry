package com.yun.methodretry.utils;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.util.HashUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 *
 *
 * @author raoliwen
 * @date 2026/01/12
 */
@Slf4j
public class SelfMethodUtilsTest {
    private static final String PATTERN = "%s-%s-%s";
    private static volatile ConcurrentHashSet<Integer> PROXY_SET = null;
    private static volatile ConcurrentHashSet<String> PROXY_SET2 = new  ConcurrentHashSet<>();
    @Test
    public void testHashCode() throws NoSuchMethodException {
        SelfMethodUtils.HelloDTO helloDTO = new SelfMethodUtils.HelloDTO();
        SelfMethodUtils.TestDTO testDTO = new SelfMethodUtils.TestDTO();
        testDTO.setName("test");
        helloDTO.setTestDTO(testDTO);
        helloDTO.setName("test");
        String beanName = "beanName";
        SelfMethodUtils.HelloService helloService = new SelfMethodUtils.HelloService();
        Method hello = helloService.getClass().getMethod("hello", SelfMethodUtils.HelloDTO.class, String.class);
        Object[] parameters = new Object[]{helloDTO, "test"};
        SelfMethodUtils.MethodCallSpec spec = SelfMethodUtils.buildMethodCallSpec(beanName,hello,parameters, 0);
        int batch = 1000000;
        // 1、使用hash
        int i = HashUtil.jsHash(String.format(PATTERN, spec.getClassName(), spec.getMethodName(), JSONUtil.toJsonStr(spec.getArgs())));
        getProxySet().add(i);
        long now = System.currentTimeMillis();
        for (int j = 0; j < batch; j++) {
            int temp = HashUtil.jsHash(String.format(PATTERN, spec.getClassName(), spec.getMethodName(), JSONUtil.toJsonStr(spec.getArgs())));
            getProxySet().contains(temp);
            getProxySet().remove(temp);
            getProxySet().add(temp);
        }
        long end = System.currentTimeMillis();
        log.info("hash cost:{}",(end-now));
        // 2、使用String
        String s = String.format(PATTERN, spec.getClassName(), spec.getMethodName(), JSONUtil.toJsonStr(spec.getArgs()));
        PROXY_SET2.add(s);
        now = System.currentTimeMillis();
        for (int j = 0; j < batch; j++) {
            String temp = String.format(PATTERN, spec.getClassName(), spec.getMethodName(), JSONUtil.toJsonStr(spec.getArgs()));
            PROXY_SET2.contains(temp);
            PROXY_SET2.remove(temp);
            PROXY_SET2.add(temp);
        }
        end = System.currentTimeMillis();
        log.info("string cost:{}",(end-now));
    }

    private static ConcurrentHashSet<Integer> getProxySet(){
        if (PROXY_SET == null) {
            synchronized (SelfMethodUtils.class) {
                if (PROXY_SET == null) {
                    PROXY_SET = new ConcurrentHashSet<>();
                }
            }
        }
        return PROXY_SET;
    }

}
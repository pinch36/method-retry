package com.yun.methodretry.utils;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.beans.Introspector;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 反射处理方法的utils
 *
 * @author raoliwen
 * @date 2025/12/29
 */
public class SelfMethodUtils {
    private static final ObjectMapper OM = new ObjectMapper();
    // 缓存：可使用缓存组件（缓存更新，热key等）
    private static volatile ConcurrentHashMap<String,Object> INSTANCE_MAP = null;
    // 传代理对象使用，区分服务调用
    private static volatile ConcurrentHashSet<String> PROXY_SET = null;
    // 唯一标识：类名+方法名+参数
    private static final String PATTERN = "%s-%s-%s";

    public static boolean cheek(MethodCallSpec methodCallSpec) {
        return getProxySet().contains(getMarkKey(methodCallSpec));
    }

    public static void removeProxyMark(MethodCallSpec methodCallSpec) {
        getProxySet().remove(getMarkKey(methodCallSpec));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    public static class MethodCallSpec {
        private String className;        // 目标类全限定名
        private String methodName;       // 方法名
        private String[] paramTypeNames; // 参数类型（全限定名或原始类型名：int/long/...）
        private Object[] args;           // 参数值（与 paramTypeNames 对应）
        private String beanName;         // springboot注册的beanName
        private LocalDateTime startTime; // 重试开始的时间
        private String extra;            // 额外信息
    }
    public static MethodCallSpec buildMethodCallSpec(String beanName, Method method, Object[] args, long startTime) {
        Objects.requireNonNull(method, "method must not be null");
        String className = method.getDeclaringClass().getName();
        SelfMethodUtils.MethodCallSpec methodCallSpec = new SelfMethodUtils.MethodCallSpec();
        methodCallSpec.setMethodName(method.getName());
        methodCallSpec.setClassName(className);
        methodCallSpec.setArgs(args == null?new Object[0]:args);
        methodCallSpec.setParamTypeNames(Arrays.stream(method.getParameterTypes()).map(Class::getName).toArray(String[]::new));
        if(StringUtils.isEmpty(beanName)){
            beanName = Introspector.decapitalize(className.substring(className.lastIndexOf('.') + 1));
        }
        methodCallSpec.setBeanName(beanName);
        LocalDateTime now = LocalDateTime.now().plusMinutes(startTime);
        methodCallSpec.setStartTime(now);
        return methodCallSpec;
    }

    public static MethodCallSpec buildMethodCallSpec(String beanName,Method method,Object[] args,String extra,long startTime) {
        SelfMethodUtils.MethodCallSpec methodCallSpec = buildMethodCallSpec(beanName, method, args, startTime);
        methodCallSpec.setExtra(extra);
        return methodCallSpec;
    }
    private static boolean validateSpecBase(MethodCallSpec spec) {
        if (Objects.isNull(spec)) {
            return true;
        }
        if (StringUtils.isBlank(spec.className)) {
            return true;
        }
        if (spec.args == null) {
            spec.args = new Object[0];
        }
        if (spec.paramTypeNames == null) {
            spec.paramTypeNames = new String[0];
        }
        if (spec.paramTypeNames.length > 0 && spec.paramTypeNames.length != spec.args.length) {
            return true;
        }
        return false;
    }

    /**
     * 代理对象方法（要做到幂等，否则可能重复触发代理导致递归）
     *
     * @param object
     * @param spec
     * @return {@link Object }
     * @throws Exception
     */
    public static Object invokeFromSpec(Object object, MethodCallSpec spec) throws Exception {
        // 1、前置校验
        Objects.requireNonNull(object, "object must not be null");
        if (validateSpecBase(spec)) {
            throw new IllegalArgumentException("methodCallSpec is invalid");
        }

        // 2、参数类型校验转化
        Class<?> clazz = object.getClass();
        Class<?>[] paramTypes = new Class<?>[spec.paramTypeNames.length];
        for (int i = 0; i < spec.paramTypeNames.length; i++) {
            paramTypes[i] = resolveType(spec.paramTypeNames[i]);
        }

        // 3、参数转换成可用的
        Object[] coercedArgs = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            coercedArgs[i] = coerceArg(spec.args[i], paramTypes[i]);
        }

        // 4、幂等：标记为已消费
        String markKey = getMarkKey(spec);
        getProxySet().add(markKey);

        return MethodUtils.invokeMethod(object,spec.getMethodName(),coercedArgs,paramTypes);
    }

    private static String getMarkKey(MethodCallSpec spec) {
        return String.format(PATTERN, spec.className, spec.methodName, JSONUtil.toJsonStr(spec.args));
    }

    /**
     * 原对象方法
     *
     * @param spec
     * @return {@link Object }
     * @throws Exception
     */
    public static Object invokeFromSpec(MethodCallSpec spec) throws Exception {
        // 1、前置校验
        if (validateSpecBase(spec)) {
            throw new IllegalArgumentException("methodCallSpec is invalid");
        }
        // 2、获取类信息，参数类型校验转化
        Class<?> clazz = Class.forName(spec.className);
        Class<?>[] paramTypes = new Class<?>[spec.paramTypeNames.length];
        for (int i = 0; i < spec.paramTypeNames.length; i++) {
            paramTypes[i] = resolveType(spec.paramTypeNames[i]);
        }

        // 3、参数转换成可用的
        Object[] coercedArgs = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            coercedArgs[i] = coerceArg(spec.args[i], paramTypes[i]);
        }

        // 4、创建原对象
        Object target = getInstance(spec.className,clazz);


        return MethodUtils.invokeMethod(target,spec.getMethodName(),coercedArgs,paramTypes);
    }
    private static Object getInstance(String className,Class<?> clazz) throws Exception {
        ConcurrentHashMap<String, Object> instanceMap = getInstanceMap();
        if (instanceMap.containsKey(className)) {
            return instanceMap.get(className);
        }
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object target = constructor.newInstance();
        instanceMap.put(className, target);
        return target;
    }
    private static ConcurrentHashSet<String> getProxySet(){
        if (PROXY_SET == null) {
            synchronized (SelfMethodUtils.class) {
                if (PROXY_SET == null) {
                    PROXY_SET = new ConcurrentHashSet<>();
                }
            }
        }
        return PROXY_SET;
    }

    private static ConcurrentHashMap<String,Object> getInstanceMap(){
        if (INSTANCE_MAP == null) {
            synchronized (SelfMethodUtils.class) {
                if (INSTANCE_MAP == null) {
                    INSTANCE_MAP = new ConcurrentHashMap<>();
                }
            }
        }
        return INSTANCE_MAP;
    }
    private static Class<?> resolveType(String typeName) throws ClassNotFoundException {
        if (StringUtils.isBlank(typeName)) {
            throw new IllegalArgumentException("typeName must not be blank");
        }
        switch (typeName) {
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "char":
                return char.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "void":
                return void.class;
            default:
                return Class.forName(typeName);
        }
    }

    private static Object coerceArg(Object arg, Class<?> targetType) {
        if (arg == null) {
            return null;
        }

        // 目标是原始类型时，转成对应包装类型再交给反射自动拆箱
        if (targetType.isPrimitive()) {
            targetType = primitiveToWrapper(targetType);
        }

        if (targetType.isInstance(arg)) {
            return arg;
        }

        // 数字转换
        if (arg instanceof Number) {
            Number n = (Number) arg;
            if (targetType == Integer.class) {
                return n.intValue();
            }
            if (targetType == Long.class) {
                return n.longValue();
            }
            if (targetType == Double.class) {
                return n.doubleValue();
            }
            if (targetType == Float.class) {
                return n.floatValue();
            }
            if (targetType == Short.class) {
                return n.shortValue();
            }
            if (targetType == Byte.class) {
                return n.byteValue();
            }
        }

        // 字符/布尔
        if (targetType == Boolean.class && arg instanceof String) {
            return Boolean.parseBoolean((String) arg);
        }
        if (targetType == Character.class && arg instanceof String) {
            String s = (String) arg;
            if (!s.isEmpty()) {
                return s.charAt(0);
            }
        }

        // 字符串兜底
        if (targetType == String.class) {
            return String.valueOf(arg);
        }

        // 复杂对象转换（不支持嵌套）
        arg = OM.convertValue(arg, targetType);
        return arg;
    }


    private static Class<?> primitiveToWrapper(Class<?> p) {
        if (p == int.class) {
            return Integer.class;
        }
        if (p == long.class) {
            return Long.class;
        }
        if (p == double.class) {
            return Double.class;
        }
        if (p == float.class) {
            return Float.class;
        }
        if (p == boolean.class) {
            return Boolean.class;
        }
        if (p == byte.class) {
            return Byte.class;
        }
        if (p == short.class) {
            return Short.class;
        }
        if (p == char.class) {
            return Character.class;
        }
        return p;
    }

    // ===========单测============
    public static void main(String[] args) throws Exception {
        HelloDTO helloDTO = new HelloDTO();
        TestDTO testDTO = new TestDTO();
        testDTO.setName("test");
        helloDTO.setTestDTO(testDTO);
        helloDTO.setName("test");
        String beanName = "beanName";
        HelloService helloService = new HelloService();
        Method hello = helloService.getClass().getMethod("hello", HelloDTO.class, String.class);
        Object[] parameters = new Object[]{helloDTO, "test"};
        SelfMethodUtils.MethodCallSpec methodCallSpec = SelfMethodUtils.buildMethodCallSpec(beanName,hello,parameters, 0);
        String jsonStr = JSONUtil.toJsonStr(methodCallSpec);
        MethodCallSpec bean = JSONUtil.toBean(jsonStr, MethodCallSpec.class);
        Object o = invokeFromSpec(helloService, bean);
        System.out.println(JSONUtil.toJsonStr(o));
    }
    @Data
    public static class HelloDTO {
        String name;
        String message;
        TestDTO testDTO;
    }
    @Data
    public static class TestDTO {
        String name;
    }
    public static class HelloService {
        public String hello(HelloDTO helloDTO,String name){
            return "hello";
        }
    }
}

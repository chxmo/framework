package com.xm.boot.redis.annotation.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

/**
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
public class SimpleKeyGenerator implements KeyGeneratorStrategy {

    private final static Logger LOGGER = LoggerFactory.getLogger(SimpleKeyGenerator.class);

    private final static String NO_PARAM_KEY = "NO_PARAM";

    private final static String NULL_PARAM_KEY = "NULL_PARAM";

    @Override
    public String generateKey(String cacheName, String[] keys, Method method, Object[] params) {
        StringBuilder key = new StringBuilder();
        if (!StringUtils.isEmpty(cacheName)) {
            key.append(cacheName).append(":");
        }
        key.append(method.getDeclaringClass().getSimpleName()).append(".").append(method.getName()).append(":");
        if (params.length == 0) {
            return key.append(NO_PARAM_KEY).toString();
        }
        for (Object param : params) {
            if (param == null) {
                LOGGER.warn("[redisLock]input null param for Redis Lock, use default key={}", NULL_PARAM_KEY);
                key.append(NULL_PARAM_KEY);
            } else if (ClassUtils.isPrimitiveArray(param.getClass())) {
                int length = Array.getLength(param);
                for (int i = 0; i < length; i++) {
                    key.append(Array.get(param, i));
                    key.append(',');
                }
            } else if (ClassUtils.isPrimitiveOrWrapper(param.getClass()) || param instanceof String) {
                key.append(param);
            } else {
                LOGGER.warn("[redisLock]Using an object as a key may lead to unexpected results. " +
                        "Method is #" + method.getName());
                key.append("hc_").append(param.hashCode());
            }
            key.append(':');
        }
        String result = key.toString();
        if (result.endsWith(":")) {
            key.deleteCharAt(key.length() - 1);
            return key.toString();
        }
        return result;
    }

}

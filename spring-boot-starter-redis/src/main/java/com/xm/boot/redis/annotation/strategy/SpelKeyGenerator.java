package com.xm.boot.redis.annotation.strategy;

import com.xm.boot.redis.util.SpelUtil;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * 通过springel解析器动态解析参数生成key名称
 *
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
public class SpelKeyGenerator implements KeyGeneratorStrategy {
    @Override
    public String generateKey(String cacheName, String[] keys, Method method, Object[] params) {
        StringBuilder result = new StringBuilder();
        if (!StringUtils.isEmpty(cacheName)) {
            result.append(cacheName).append(":");
        }
        for (String key : keys) {
            result.append(SpelUtil.parse(key, method, params));
            result.append(":");
        }
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }
}

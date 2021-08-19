package com.xm.boot.redis.annotation.strategy;

import java.lang.reflect.Method;

/**
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
public interface KeyGeneratorStrategy {

    /**
     * 自动生成key名称
     *
     * @param cacheName @RedisLock的cacheName的值
     * @param keys      @RedisLock的keys的值
     * @param method    目标方法
     * @param params    方法参数
     * @return
     */
    String generateKey(String cacheName, String[] keys, Method method, Object[] params);
}

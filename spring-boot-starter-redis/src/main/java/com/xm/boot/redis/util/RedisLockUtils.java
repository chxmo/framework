package com.xm.boot.redis.util;

import com.xm.boot.redis.lock.DistributedLock;
import com.xm.boot.redis.lock.RedisDistributedLock;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
@Slf4j
public class RedisLockUtils {

    private static RedisDistributedLock redisDistributedLock;

    public static void initRedisDistributedLock(RedisDistributedLock lock) {
        redisDistributedLock = lock;
    }

    /**
     * 默认超时时间和重试次数
     * 默认重试次数100次，每次休眠500ms-自旋锁
     * 默认锁持有时间60s
     *
     * @param key
     * @return
     */
    public static boolean lock(String key) {
        return redisDistributedLock.lock(key);
    }

    /**
     * 自定义配置过期时间
     *
     * @param key
     * @param expire 单位秒（注意：锁持有时间）
     * @return
     */
    public static boolean lock(String key, long expire) {
        return redisDistributedLock.lock(key, expire);
    }

    /**
     * 尝试获取锁，直到timeout如果还未获取到，则返回false
     *
     * @param key
     * @param timeout 单位秒 注意：该字段表示自旋获取锁的最大时间
     * @return
     */
    public static boolean tryLock(String key, long timeout) {
        int retry = (int) (timeout * 1000 / DistributedLock.SLEEP_MILLIS);
        return redisDistributedLock.lock(key, retry, DistributedLock.SLEEP_MILLIS);
    }

    /**
     * 尝试获取锁,直到timeout如果还未获取到，则返回false;如果返回true，则设置过期时间
     *
     * @param key
     * @param expire  过期时间
     * @param timeout 单位秒 注意：该字段表示自旋获取锁的最大时间
     * @return
     */
    public static boolean tryLock(String key, long expire, long timeout) {
        int retry = (int) (timeout * 1000 / DistributedLock.SLEEP_MILLIS);
        return redisDistributedLock.lock(key, expire, retry, DistributedLock.SLEEP_MILLIS);
    }

    /**
     * 释放锁
     *
     * @param key
     * @return
     */
    public static boolean unlock(String key) {
        return redisDistributedLock.releaseLock(key);
    }

}

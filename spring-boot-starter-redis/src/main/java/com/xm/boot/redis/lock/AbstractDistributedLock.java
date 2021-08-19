package com.xm.boot.redis.lock;

import com.xm.boot.redis.config.RedisConfig;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 分布式锁-抽象类
 *
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
public abstract class AbstractDistributedLock implements DistributedLock {

    @Autowired
    private RedisConfig redisConfig;

    @Override
    public boolean lock(String key) {
        return lock(key, TIMEOUT_MILLIS, RETRY_TIMES, SLEEP_MILLIS);
    }

    @Override
    public boolean lock(String key, int retryTimes) {
        return lock(key, TIMEOUT_MILLIS, retryTimes, SLEEP_MILLIS);
    }

    @Override
    public boolean lock(String key, int retryTimes, long sleepMillis) {
        return lock(key, TIMEOUT_MILLIS, retryTimes, sleepMillis);
    }

    @Override
    public boolean lock(String key, long expire) {
        return lock(key, expire, RETRY_TIMES, SLEEP_MILLIS);
    }

    @Override
    public boolean lock(String key, long expire, int retryTimes) {
        return lock(key, expire, retryTimes, SLEEP_MILLIS);
    }

    @Override
    public boolean lock(String key, long expire, int retryTimes, long sleepMillis) {
        String realKey = this.generateKeyName(key);
        expire = expire * 1000;//过期时间 秒转换为毫秒
        return this.doLock(realKey, expire, retryTimes, sleepMillis);
    }

    @Override
    public boolean releaseLock(String key) {
        return this.doReleaseLock(generateKeyName(key));
    }

    @Override
    public String generateKeyName(String key) {
        return String.format(redisConfig.getPrefix() + ":" + PREFIX, key);
    }

    abstract boolean doReleaseLock(String key);

    abstract boolean doLock(String key, long expire, int retryTimes, long sleepMillis);

}

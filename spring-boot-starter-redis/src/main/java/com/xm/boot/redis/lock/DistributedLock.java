package com.xm.boot.redis.lock;

/**
 * 分布式锁-接口
 *
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
public interface DistributedLock {

    /**
     * 锁前缀
     */
    String PREFIX = "lock:%s";

    /**
     * 默认持有锁超时时间 1*60s
     */
    long TIMEOUT_MILLIS = 1 * 60;

    /**
     * 重试次数
     */
    int RETRY_TIMES = 10;

    /**
     * 休眠时间
     */
    long SLEEP_MILLIS = 500;

    boolean lock(String key);

    boolean lock(String key, int retryTimes);

    boolean lock(String key, int retryTimes, long sleepMillis);

    boolean lock(String key, long expire);

    boolean lock(String key, long expire, int retryTimes);

    /**
     * lock 底层接口
     *
     * @param key         锁名
     * @param expire      持有锁过期时间
     * @param retryTimes  锁竞争时重试次数
     * @param sleepMillis 锁竞争重试延时时间
     * @return
     */
    boolean lock(String key, long expire, int retryTimes, long sleepMillis);

    boolean releaseLock(String key);

    String generateKeyName(String key);
}

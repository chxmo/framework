package com.xm.boot.redis.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分布式锁-实现类
 *
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
@Slf4j
public class RedisDistributedLock extends AbstractDistributedLock {

    /**
     * 加锁lua脚本
     */
    public static final String LOCK_LUA;
    /**
     * 解锁lua脚本
     */
    public static final String UNLOCK_LUA;

    static {
        StringBuilder lockStringBuilder = new StringBuilder();
        // 检查是否key已经被占用，如果没有则设置超时时间和唯一标识，初始化value=1
        lockStringBuilder.append("if (redis.call('exists', KEYS[1]) == 0) then ");
        lockStringBuilder.append("  redis.call('hset', KEYS[1], ARGV[2], 1); ");
        lockStringBuilder.append("  redis.call('pexpire', KEYS[1], ARGV[1]); ");
        lockStringBuilder.append("  return nil; ");
        lockStringBuilder.append("end; ");
        // 如果锁重入,需要判断锁的key field 都一直情况下 value 加一
        lockStringBuilder.append("if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then ");
        lockStringBuilder.append("  redis.call('hincrby', KEYS[1], ARGV[2], 1); ");
        //锁重入重新设置超时时间
        lockStringBuilder.append("  redis.call('pexpire', KEYS[1], ARGV[1]); ");
        lockStringBuilder.append("  return nil;");
        lockStringBuilder.append("end; ");
        // 返回剩余的过期时间
        lockStringBuilder.append("return redis.call('pttl', KEYS[1]);");
        LOCK_LUA = lockStringBuilder.toString();

        StringBuilder unlockStringBuilder = new StringBuilder();
        // key和field不匹配，说明当前客户端线程没有持有锁，不能主动解锁。
        unlockStringBuilder.append("if (redis.call('hexists', KEYS[1], ARGV[1]) == 0) then  ");
        unlockStringBuilder.append("  return nil; ");
        unlockStringBuilder.append("end;  ");
        // 将value减1
        unlockStringBuilder.append("local counter = redis.call('hincrby', KEYS[1], ARGV[1], -1);  ");
        // 如果counter>0说明锁在重入，不能删除key
        unlockStringBuilder.append("if (counter > 0) then  ");
        unlockStringBuilder.append("  return 0;  ");
        unlockStringBuilder.append("else  ");
        unlockStringBuilder.append("  redis.call('del', KEYS[1]); ");
        unlockStringBuilder.append("  return 1; ");
        unlockStringBuilder.append("end; ");
        unlockStringBuilder.append("return nil;");
        UNLOCK_LUA = unlockStringBuilder.toString();
    }

    private RedisTemplate redisTemplate;
    private ThreadLocal<Map<String, String>> lockFlag = new ThreadLocal<Map<String, String>>();

    public RedisDistributedLock(RedisTemplate redisTemplate) {
        super();
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean doLock(String key, long expire, int retryTimes, long sleepMillis) {
        int totalRetryTimes = retryTimes;//总次数
        boolean result = redisLock(key, expire);
        // 如果获取锁失败，按照传入的重试次数进行重试
        while ((!result) && retryTimes-- > 0) {
            try {
                log.debug(key + "lock failed, retrying..." + (totalRetryTimes - retryTimes) + " sleepMillis:" + sleepMillis);
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                log.error("redis doLock", e);
                return false;
            }
            result = redisLock(key, expire);
        }
        log.debug(key + " 上锁 " + result);
        return result;
    }

    /**
     * 通过threadlocal来判断该锁是否是重入
     *
     * @param key
     * @param expire 单位秒
     * @return
     */
    private boolean redisLock(String key, Long expire) {
        try {
            Map<String, String> uuids = lockFlag.get();
            String uuid = null;
            if (null == uuids) {
                //表示第一次进入
                uuids = new ConcurrentHashMap<>();
                uuid = UUID.randomUUID().toString() + ":" + Thread.currentThread().getId();
                uuids.put(key, uuid);
                lockFlag.set(uuids);
            } else if (null == uuids.get(key)) {
                uuid = UUID.randomUUID().toString() + ":" + Thread.currentThread().getId();
                uuids.put(key, uuid);
            } else {
                uuid = uuids.get(key);
            }
            RedisScript<Long> redisScript = new DefaultRedisScript<Long>(LOCK_LUA, Long.class);
            Long result = (Long) redisTemplate.execute(redisScript, Collections.singletonList(key), Integer.parseInt(expire.toString()), uuid);
            return StringUtils.isEmpty(result);
        } catch (Exception e) {
            log.error("set redis occured an exception:{}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean doReleaseLock(String key) {
        // 释放锁的时候，有可能因为持锁之后方法执行时间大于锁的有效期，此时有可能已经被另外一个线程持有锁，所以不能直接删除
        try {
            Map<String, String> flags = lockFlag.get();
            String flag = null;
            if (null == flags || null == flags.get(key)) {
                log.error("该线程不是持有该key:{} 权限，非法解锁！", key);
                return false;
            }
            //flag为当前线程当前key
            flag = flags.get(key);
            RedisScript<Long> redisScript = new DefaultRedisScript<Long>(UNLOCK_LUA, Long.class);
            Long result = (Long) redisTemplate.execute(redisScript, Collections.singletonList(key), flag);
            boolean res = result != null && result > 0;
            log.debug("{} 解锁:{}", key, res);
            if (res) {
                //解锁成功，清楚threadlocal
                flags.remove(key);
                if (flags.isEmpty()) {
                    //只有当size为0的时候，才表示该线程已经全部解锁所有的key
                    lockFlag.remove();
                }
            }
            return res;
        } catch (Exception e) {
            log.error("release lock occured an exception:{}", e.getMessage());
        }
        return false;
    }
}

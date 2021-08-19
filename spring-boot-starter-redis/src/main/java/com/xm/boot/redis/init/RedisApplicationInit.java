package com.xm.boot.redis.init;

import com.xm.boot.redis.config.RedisConfig;
import com.xm.boot.redis.lock.RedisDistributedLock;
import com.xm.boot.redis.util.RedisLockUtils;
import com.xm.boot.redis.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.xm.boot.redis.common.Constant.FORMAT_STRING;

/**
 * 初始化redis
 *
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
@Slf4j
public class RedisApplicationInit implements InitializingBean {

    public static AtomicBoolean start = new AtomicBoolean(false);

    @Autowired
    private RedisConfig redisConfig;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedisDistributedLock redisDistributedLock;

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("application event ready...");
        if (!start.get()) {
            try {
                String prefix = redisConfig.getPrefix() + FORMAT_STRING;
                RedisUtils.init(redisConfig.getPrefix(), prefix, redisTemplate);
                log.info("redis初始化项目前缀prefix:{}", prefix);
                RedisLockUtils.initRedisDistributedLock(redisDistributedLock);
                start.getAndSet(true);
            } catch (Exception ex) {
                log.debug("初始化redis失败", ex.getLocalizedMessage());
            }
            log.info("redis初始化完成");
        }
    }
}

package com.xm.boot.redis.annotation;


import com.xm.boot.redis.annotation.strategy.KeyGeneratorLoader;
import com.xm.boot.redis.exception.RedisExceptionEnum;
import com.xm.boot.redis.exception.RedisRuntimeException;
import com.xm.boot.redis.util.RedisLockUtils;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.util.StringUtils;

/**
 * 分布式锁自定义注解拦截器
 *
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
public class LockInterceptor implements MethodInterceptor {

    /**
     * key name 生成器
     */
    private KeyGeneratorLoader keyGeneratorLoader;

    public LockInterceptor(KeyGeneratorLoader keyGeneratorLoader) {
        this.keyGeneratorLoader = keyGeneratorLoader;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        RedisLock annotation = invocation.getMethod().getAnnotation(RedisLock.class);
        String key = keyGeneratorLoader.generateKey(annotation.cacheName(), annotation.keys(), invocation.getMethod(), invocation.getArguments());
        if (StringUtils.isEmpty(key)) {
            throw new RedisRuntimeException(RedisExceptionEnum.REDIS_KEY_NOT_EMPTY);
        }
        boolean result = RedisLockUtils.tryLock(key, annotation.expire(), annotation.timeout());
        try {
            if (result) {
                return invocation.proceed();
            }
            throw new RedisRuntimeException(RedisExceptionEnum.REDIS_LOCK_FAIL);
        } finally {
            //解锁
            if (result) {
                RedisLockUtils.unlock(key);
            }
        }
    }
}

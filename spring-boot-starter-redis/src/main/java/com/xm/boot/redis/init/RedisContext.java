package com.xm.boot.redis.init;

import com.xm.boot.redis.exception.RedisExceptionEnum;
import com.xm.boot.redis.exception.RedisRuntimeException;
import org.springframework.util.StringUtils;

/**
 * redis配置 上下文
 *
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
public class RedisContext {

    private final static ThreadLocal<RedisContext> REDIS_CONTEXT_THREAD_LOCAL = ThreadLocal.withInitial(RedisContext::new);

    /**
     * Redis 缓存前缀
     * <p></>优先级高于 yml/xml 配置中的前缀，用于处理其他命名空间下的redis前缀操作</p>
     */
    private String prefix = null;

    /**
     * 获取Redis context
     *
     * @return
     */
    public static RedisContext getRedisContext() {
        return REDIS_CONTEXT_THREAD_LOCAL.get();
    }

    /**
     * 清空redis上下文数据
     */
    public static void clear() {
        REDIS_CONTEXT_THREAD_LOCAL.remove();
    }

    /**
     * 设置前缀
     *
     * @return
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * 设置缓存前缀
     *
     * @param prefix
     */
    public static void setPrefix(String prefix) {
        if (StringUtils.isEmpty(prefix)) {
            throw new RedisRuntimeException(RedisExceptionEnum.REDIS_PREFIX_KEY_NOT_EMPTY);
        }
        REDIS_CONTEXT_THREAD_LOCAL.get().prefix = prefix;
    }

    @Override
    public String toString() {
        return "RedisContext{" +
                "prefix='" + prefix + '\'' +
                '}';
    }
}

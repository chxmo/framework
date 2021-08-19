package com.xm.boot.redis.exception;

/**
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
public class RedisRuntimeException extends RuntimeException {

    private String code;

    public RedisRuntimeException(RedisExceptionEnum redisExceptionEnum) {
        super(redisExceptionEnum.toString());
        this.code = redisExceptionEnum.getCode();
    }

    public String getCode() {
        return code;
    }
}

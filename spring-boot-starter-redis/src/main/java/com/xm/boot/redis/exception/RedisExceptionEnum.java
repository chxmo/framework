package com.xm.boot.redis.exception;

/**
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
public enum RedisExceptionEnum {
    REDIS_LOCK_FAIL("10001", "分布式锁上锁失败"),
    REDIS_KEY_NOT_EMPTY("10002", "分布式锁key不为空字符"),
    REDIS_PREFIX_KEY_NOT_EMPTY("10003", "缓存前缀不能为空"),
    REDIS_PREFIX_LIMIT_NOT_SUPPORT("10004", "limit值不支持"),
    REDIS_PARAM_KEY_NOT_EMPTY("10005", "参数不能为空");

    /**
     * 错误编码
     */
    private String code;

    /**
     * 错误描述
     */
    private String description;

    RedisExceptionEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "[" + this.code + "]" + this.description;
    }
}

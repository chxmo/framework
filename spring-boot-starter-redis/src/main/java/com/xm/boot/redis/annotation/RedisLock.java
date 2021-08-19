package com.xm.boot.redis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * redislock 注解
 *
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
@Target(value = {ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface RedisLock {

    /**
     * 用于表示最小颗粒度的缓存前缀，默认为空
     */
    String cacheName() default "";

    /**
     * 生成key名规则：如果为空，默认方法名+所有参数；如果不为空，则利用SpEL规则获取动态的key名.
     * <p>如果默认是{},则采用自动模式，用方法和参数来生成key名,生成的key名为  cacheName:方法名:参数值，其中参数值有如下几种情况：</p>
     * <pre>
     *     1. 如果方法没有参数，则使用 NO_PARAM;
     *     2. 如果方法参数中存在NULL，则使用 NULL_PARAM;
     *     3. 如果方法参数中存在包装类，则使用 包装类的值；
     *     4. 如果反复参数中存在对象，并且对象不为空，则使用 hc_+对象的hashcode;
     * </pre>
     * <p>如果参数为单个动态参数,例如参数如果是对象@RedisLock(keys ={"'orderNo:'+#order.orderNo"})，如果参数是基本类型@RedisLock(keys = {"'orderNo:'+#orderNo"})，
     * 则表示从方法的参数中动态获取值,生成的key名为cacheName:orderNo(orderNo的参数值) .</p>
     * <p>如果参数为多个动态参数，例如@RedisLock(keys = {"'userId:'+#userId","'orderNo:'+#orderNo"})，则表示从方法参数中动态取值，生成的key名为：cacheName:userId(userId的参数值):orderNo(orderNo的参数值)</p>
     */
    String[] keys() default {};

    /**
     * 过期时间 单位：秒
     * <pre>
     *     过期时间一定是要长于业务的执行时间.
     * </pre>
     */
    long expire() default 60;

    /**
     * 获取锁超时时间 单位：秒
     * <pre>
     *     结合业务,建议该时间不宜设置过长,特别在并发高的情况下.
     * </pre>
     */
    long timeout() default 30;
}
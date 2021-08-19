package com.xm.boot.redis.annotation.strategy;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * 自定义key名生成器
 *
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
public class KeyGeneratorLoader implements ApplicationContextAware, KeyGeneratorStrategy {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    @Override
    public String generateKey(String cacheName, String[] keys, Method method, Object[] params) {
        KeyGeneratorStrategy keyGeneratorStrategy = null;
        if (StringUtils.isEmpty(keys) || 0 == keys.length) {
            //采用simp模式生成
            keyGeneratorStrategy = applicationContext.getBean(SimpleKeyGenerator.class);
        } else {
            //采用spel动态参数生成
            keyGeneratorStrategy = applicationContext.getBean(SpelKeyGenerator.class);
        }
        Assert.notNull(keyGeneratorStrategy, "KeyGeneratorStrategy is null!!!");
        return keyGeneratorStrategy.generateKey(cacheName, keys, method, params);
    }
}

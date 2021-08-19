package com.xm.boot.redis.annotation;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.Ordered;

/**
 * 分布式锁advisor通知
 *
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
public class LockAnnotationAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {

    private Advice advice;

    private Pointcut pointcut;

    public LockAnnotationAdvisor(LockInterceptor lockInterceptor) {
        this.advice = lockInterceptor;
        this.pointcut = buildPointcut();
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

    @Override
    public Advice getAdvice() {
        return this.advice;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (this.advice instanceof BeanFactoryAware) {
            ((BeanFactoryAware) this.advice).setBeanFactory(beanFactory);
        }
    }

    /**
     * 构造ponitcut
     *
     * @return
     */
    private Pointcut buildPointcut() {
        return AnnotationMatchingPointcut.forMethodAnnotation(RedisLock.class);
    }

    @Override
    public void setOrder(int order) {
        super.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }
}

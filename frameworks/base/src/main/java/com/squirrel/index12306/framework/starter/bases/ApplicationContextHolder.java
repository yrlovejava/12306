package com.squirrel.index12306.framework.starter.bases;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Application context holder
 */
public class ApplicationContextHolder implements ApplicationContextAware {

    private static ApplicationContext CONTEXT;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextHolder.CONTEXT = applicationContext;
    }

    /**
     * 根据类型获取 ioc 容器中的bean
     * @param clazz 类型
     * @return bean
     * @param <T> 泛型
     */
    public static <T> T getBean(Class<T> clazz) {
        return CONTEXT.getBean(clazz);
    }

    /**
     * 根据类型和名称从 ioc 容器中获取bean
     * @param name bean名称
     * @param clazz 类型
     * @return bean
     * @param <T> 泛型
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return CONTEXT.getBean(name, clazz);
    }

    /**
     * 在 ioc 容器中获取指定类型的所有bean实例
     * @param clazz 类型
     * @return 所有bean实例
     * @param <T> 泛型
     */
    public static <T> Map<String,T> getBeansOfType(Class<T> clazz) {
        return CONTEXT.getBeansOfType(clazz);
    }

    /**
     * 在 ioc 容器中获取带有指定类型注解和指定名称的bean实例
     * @param beanName bean名称
     * @param annotationType 注解类型
     * @return bean实例
     * @param <A> 泛型
     */
    public static <A extends Annotation> A findAnnotationOnBean(String beanName,Class<A> annotationType) {
        return CONTEXT.findAnnotationOnBean(beanName,annotationType);
    }

    /**
     * 获取 applicationContext
     * @return ApplicationContext
     */
    public static ApplicationContext getInstance() {
        return CONTEXT;
    }
}

package com.dutycode.springboot.interceptorextends.anno;

import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.annotation.*;
/**
 * 基线拦截器注解， 拦截器注解需要实现此注解
 * @author zhangzhonghua
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BaseInterceptor {

    /**
     * 拦截器实现类
     * @return
     */
    Class<? extends HandlerInterceptor> value();

}



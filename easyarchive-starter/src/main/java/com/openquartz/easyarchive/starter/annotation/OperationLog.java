package com.openquartz.easyarchive.starter.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作描述
     */
    String value() default "";

    /**
     * 操作类型
     */
    String type() default "OTHER";

    /**
     * 是否记录参数
     */
    boolean logParams() default true;
}
package com.zion.system.annotation;

import java.lang.annotation.*;

/**
 * 防重复提交注解
 * 在需要防重复提交的 Controller 方法上添加此注解
 *
 * 使用示例:
 *   @RepeatSubmit                    // 默认3秒内不可重复提交
 *   @RepeatSubmit(interval = 5000)   // 5秒内不可重复提交
 *   @RepeatSubmit(message = "正在处理中，请勿重复操作")  // 自定义提示
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {

    /**
     * 间隔时间（毫秒），在此时间内不允许重复提交
     * 默认 3000ms（3秒）
     */
    int interval() default 3000;

    /**
     * 提示消息
     */
    String message() default "请勿重复提交";
}

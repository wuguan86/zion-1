package com.zion.crypto;

import java.lang.annotation.*;

/**
 * 响应加密注解
 * 标注此注解的方法返回数据会被加密
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EncryptResponse {
}

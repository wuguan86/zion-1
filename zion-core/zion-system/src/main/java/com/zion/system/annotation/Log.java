package com.zion.system.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /**
     * 模块名称
     */
    String title() default "";

    /**
     * 业务类型（0其它 1新增 2修改 3删除 4查询 5导出 6导入）
     */
    BusinessType businessType() default BusinessType.OTHER;

    /**
     * 是否保存请求参数
     */
    boolean isSaveRequestData() default true;

    /**
     * 是否保存响应数据
     */
    boolean isSaveResponseData() default true;

    /**
     * 业务类型枚举
     */
    enum BusinessType {
        /**
         * 其它
         */
        OTHER(0),
        /**
         * 新增
         */
        INSERT(1),
        /**
         * 修改
         */
        UPDATE(2),
        /**
         * 删除
         */
        DELETE(3),
        /**
         * 查询
         */
        QUERY(4),
        /**
         * 导出
         */
        EXPORT(5),
        /**
         * 导入
         */
        IMPORT(6);

        private final int value;

        BusinessType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}

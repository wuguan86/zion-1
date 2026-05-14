package com.zion.job.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 示例定时任务
 */
@Slf4j
@Component("sampleTask")
public class SampleTask {

    /**
     * 无参数任务
     */
    public void noParams() {
        log.info("执行无参数定时任务...");
    }

    /**
     * 有参数任务
     */
    public void withParams(String message) {
        log.info("执行有参数定时任务，参数：{}", message);
    }

    /**
     * 多参数任务
     */
    public void multipleParams(String s, Boolean b, Long l, Double d, Integer i) {
        log.info("执行多参数定时任务，参数：s={}, b={}, l={}, d={}, i={}", s, b, l, d, i);
    }
}

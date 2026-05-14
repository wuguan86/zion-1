package com.zion.system.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 代码领取微信公众号【程序员Zion】
 *
 * @className: TestTask
 * @author: Zion
 * @date: 2026/3/3 20:55
 */
@Slf4j
@Component
public class TestTask {

    /**
     * 测试任务执行
     */
    public void test() {
        log.info("测试定时任务");
    }
}

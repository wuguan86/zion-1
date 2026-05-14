package com.zion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Zion Admin 启动类
 */
@SpringBootApplication
@EnableScheduling
public class ZionAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZionAdminApplication.class, args);
    }
}

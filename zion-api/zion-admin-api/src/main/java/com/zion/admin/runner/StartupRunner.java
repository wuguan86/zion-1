package com.zion.admin.runner;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

/**
 * 启动完成后打印访问地址
 */
@Component
@RequiredArgsConstructor
public class StartupRunner implements ApplicationRunner {

    private final Environment env;

    @Override
    public void run(ApplicationArguments args) {
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String hostAddress = "localhost";

        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            // 忽略异常，使用默认值
        }

        System.out.println("\n=================================================");
        System.out.println("        Zion Admin 启动成功!");
        System.out.println("=================================================");
        System.out.println("  本地访问: http://localhost:" + port + contextPath);
        System.out.println("  网络访问: http://" + hostAddress + ":" + port + contextPath);
        System.out.println("=================================================\n");
    }
}

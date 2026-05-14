package com.zion.system.config;

import com.zion.system.interceptor.ApiAccessCollectInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API 访问统计 Web 配置
 */
@Configuration
@RequiredArgsConstructor
public class ApiAccessLogWebConfig implements WebMvcConfigurer {

    private final ApiAccessCollectInterceptor apiAccessCollectInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiAccessCollectInterceptor)
                .addPathPatterns("/api/**")
                .order(10);  // 在 Sa-Token 之后执行
    }
}

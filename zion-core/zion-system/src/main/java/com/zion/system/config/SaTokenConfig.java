package com.zion.system.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token配置
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 注册Sa-Token拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
            // 登录校验 - 只拦截 /api/** 路径
            SaRouter.match("/api/**")
                    .notMatch(
                            "/api/auth/login",
                            "/api/auth/register",           // 用户注册
                            "/api/auth/captcha",            // 图片验证码
                            "/api/auth/sms-code",           // 短信验证码
                            "/api/app/auth/login",          // App端登录
                            "/api/app/auth/sms-code",       // App端短信验证码
                            "/api/wechat/miniprogram/**",   // 微信小程序接口
                            "/api/mall/home",               // 小程序首页
                            "/api/mall/login",              // 小程序登录
                            "/api/mall/loginByPhone",       // 小程序登录
                            "/api/crypto/**",               // 加密配置
                            "/api/sys/config-group/public", // 公开配置
                            "/api/sys/user/template",       // 模板下载
                            "/api/sys/file/preview/**",     // 文件预览
                            "/api/sys/file/download/**",    // 文件下载
                            "/api/file/**",                 // 文件访问
                            "/api/files/**"                 // 文件访问
                    )
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/api/**");  // 只拦截 API 路径，不拦截静态资源
    }

    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

package com.seecoder.TomatoMall.configure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyWebMvcConfig implements WebMvcConfigurer {
    @Autowired
    LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/accounts/login",   // 放行登录
                        "/api/accounts/register",// 新增：放行注册
                        "/api/accounts",         // 放行POST注册（根据实际接口调整）
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/v2/api-docs"
                )
                .order(1);
    }
}

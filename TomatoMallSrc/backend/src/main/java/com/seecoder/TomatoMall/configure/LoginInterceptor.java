package com.seecoder.TomatoMall.configure;

import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    TokenUtil tokenUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.toString().equals(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        String method = request.getMethod();
        System.out.println(uri);
        System.out.println(method);
        if ("/api/accounts".equals(uri) && "POST".equalsIgnoreCase(method)) {
            return true;
        }
        String token = request.getHeader("token");

        if (token != null && tokenUtil.verifyToken(token)) {
            System.out.println("!!!Login token check!!!");
            request.getSession().setAttribute("currentAccount", tokenUtil.getAccount(token));
            return true;
        } else {
            throw TomatoMallException.notLogin();
        }
    }

}

package com.seecoder.TomatoMall.configure;

import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
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
        String token = request.getHeader("token");
        if (token != null && tokenUtil.verifyToken(token)) {
            request.getSession().setAttribute("currentAccount",tokenUtil.getAccount(token));
            return true;
        }else {
            throw TomatoMallException.notLogin();
        }
    }

}

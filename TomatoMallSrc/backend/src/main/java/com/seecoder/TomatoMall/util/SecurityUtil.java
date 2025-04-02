package com.seecoder.TomatoMall.util;

import com.seecoder.TomatoMall.po.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class SecurityUtil {

    @Autowired
    HttpServletRequest httpServletRequest;

    public Account getCurrentAccount() {
        Account res = (Account) httpServletRequest.getSession().getAttribute("currentAccount");
        return res;
    }

}

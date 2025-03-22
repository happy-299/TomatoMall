package com.seecoder.TomatoMall.exception;

public class TomatoMallException extends RuntimeException {
    public TomatoMallException(String message) {
        super(message);
    }

    public static TomatoMallException loginFail() {
        return new TomatoMallException("用户不存在/用户密码错误");
    }

    public static TomatoMallException usernameAlreadyExists() {
        return new TomatoMallException("用户名已存在");
    }

    public static TomatoMallException notLogin() {
        return new TomatoMallException("请登陆");
    }

}

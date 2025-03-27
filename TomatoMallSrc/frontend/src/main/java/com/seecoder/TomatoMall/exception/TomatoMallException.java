package com.seecoder.TomatoMall.exception;

public class TomatoMallException extends RuntimeException
{
    public TomatoMallException(String message)
    {
        super(message);
    }

    public static TomatoMallException loginFail()
    {
        return new TomatoMallException("用户不存在/用户密码错误");
    }

    public static TomatoMallException usernameAlreadyExists()
    {
        return new TomatoMallException("用户名已存在");
    }

    public static TomatoMallException notLogin()
    {
        return new TomatoMallException("请登陆");
    }

    public static TomatoMallException noFileContent()
    {
        return new TomatoMallException("上传的文件不能为空");
    }

    public static TomatoMallException ossInternalWrong()
    {
        return new TomatoMallException("阿里云oss服务可能存在内部错误");
    }

}

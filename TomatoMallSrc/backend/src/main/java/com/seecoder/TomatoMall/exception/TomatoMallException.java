package com.seecoder.TomatoMall.exception;

public class TomatoMallException extends RuntimeException
{
    public TomatoMallException(String message)
    {
        super(message);
        this.code = "400";
    }

    public TomatoMallException(String message, String code)
    {
        super(message);
        this.code = code;
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
        return new TomatoMallException("请登陆", "401");
    }

    public static TomatoMallException noFileContent()
    {
        return new TomatoMallException("上传的文件不能为空");
    }

    public static TomatoMallException ossInternalWrong()
    {
        return new TomatoMallException("阿里云oss服务可能存在内部错误");
    }

    public static TomatoMallException productNotFound()
    {
        return new TomatoMallException("商品不存在");
    }

    public static TomatoMallException orderStatusInvalid()
    {
        return new TomatoMallException("订单状态无效");
    }

    public static TomatoMallException quantityInvalid()
    {
        return new TomatoMallException("商品数量不可为0或负数");
    }

    public static TomatoMallException quantityMoreThanAmount()
    {
        return new TomatoMallException("商品数量超出库存数");
    }

    public static TomatoMallException cartItemExists()
    {
        return new TomatoMallException("购物车中已有商品，不可重复添加");
    }

    public static TomatoMallException cartItemNotExists()
    {
        return new TomatoMallException("购物车商品不存在");
    }

    public static TomatoMallException alipaySignVerifyFailed()
    {
        return new TomatoMallException("支付宝签名验证失败");
    }

    public static TomatoMallException alipayInnerError()
    {
        return new TomatoMallException("支付宝内部错误");
    }

    public static TomatoMallException orderNotFound()
    {
        return new TomatoMallException("订单不存在");
    }

    public static TomatoMallException alipayUnknownError()
    {
        return new TomatoMallException("支付宝出了莫名其妙的错误:支付金额不等，或者对非进行中的订单进行支付");
    }

    public static TomatoMallException reduceStockpileFailed()
    {
        return new TomatoMallException("减少库存失败");
    }

    public static TomatoMallException optimisticLockConflict()
    {
        return new TomatoMallException("库存扣减冲突，请稍后重试", "409"); // HTTP 409 Conflict
    }

    public static TomatoMallException adNotFound()
    {
        return new TomatoMallException("广告不存在");
    }


    public String getErrCode()
    {
        return code;
    }

    private final String code;
}

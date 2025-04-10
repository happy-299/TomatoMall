package com.seecoder.TomatoMall.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.service.OrderService;
import com.seecoder.TomatoMall.vo.Response;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/orders")
public class OrderController
{

    @Autowired
    OrderService orderService;

    @Data
    public class AliPay
    {
        private String traceNo;// 我们自己生成的订单编号
        private double totalAmount;// 订单的总金额
        private String subject;// 支付的名称
        private String alipayTraceNo;

    }

    @Data
    public static class RetPay
    {
        private String paymentForm;
        private String orderId;
        private BigDecimal totalAmount;
        private String paymentMethod = "Alipay";
    }

    @PostMapping("{orderId}/pay")
    public Response<RetPay> pay(@PathVariable Integer orderId)
    {
        return Response.buildSuccess(orderService.pay(orderId));
    }


    @PostMapping("/notify")
    public void handleAlipayNotify(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        orderService.handleAlipayNotify(request, response);
    }


    @GetMapping("/returnUrl")
    public String returnUrl()
    {
        System.out.println("=======================================");
        System.out.println("++后端接受到支付宝成功回调的通知，支付成功完成++");
        System.out.println("=======================================");
        return "支付成功";
    }
}

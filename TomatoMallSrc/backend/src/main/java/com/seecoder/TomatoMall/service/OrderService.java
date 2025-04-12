package com.seecoder.TomatoMall.service;


import com.seecoder.TomatoMall.controller.OrderController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface OrderService
{
    OrderController.RetPay pay(Integer orderId);

    void handleAlipayNotify(HttpServletRequest request, HttpServletResponse response) throws IOException;

    String deleteOrder(Integer orderId);
}

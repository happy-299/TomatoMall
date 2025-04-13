package com.seecoder.TomatoMall.serviceImpl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.seecoder.TomatoMall.controller.OrderController;
import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.po.Order;
import com.seecoder.TomatoMall.repository.OrderRepository;
import com.seecoder.TomatoMall.repository.StockpileRepository;
import com.seecoder.TomatoMall.service.OrderService;
import com.seecoder.TomatoMall.util.OssUtil;
import com.seecoder.TomatoMall.util.SecurityUtil;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@ConfigurationProperties(prefix = "alipay")
@Getter
@Setter
public class OrderServiceImpl implements OrderService
{
    private final OssUtil ossUtil;
    private final OrderRepository orderRepository;
    private final SecurityUtil securityUtil;
    private final UtilServiceImpl utilServiceImpl;
    private final StockpileRepository stockpileRepository;
    private final ProductServiceImpl productServiceImpl;
    private String appId;
    private String privateKey;
    private String alipayPublicKey;
    private String serverUrl;
    private String charset;
    private String signType;
    private String notifyUrl;
    private String returnUrl;

    private static final String FORMAT = "JSON";

    public OrderServiceImpl(OssUtil ossUtil, OrderRepository orderRepository, SecurityUtil securityUtil, UtilServiceImpl utilServiceImpl, StockpileRepository stockpileRepository, ProductServiceImpl productServiceImpl)
    {
        this.ossUtil = ossUtil;
        this.orderRepository = orderRepository;
        this.securityUtil = securityUtil;
        this.utilServiceImpl = utilServiceImpl;
        this.stockpileRepository = stockpileRepository;
        this.productServiceImpl = productServiceImpl;
    }


    public void handleAlipayNotify(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        System.out.println("触发支付宝回调函数handleAlipayNotify");
        // 1. 解析支付宝回调参数（通常是 application/x-www-form-urlencoded）
        Map<String, String> params = request.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));

        // 2. 验证支付宝签名（防止伪造请求）
        boolean signVerified;
        try
        {
            signVerified = AlipaySignature.rsaCheckV1(params, alipayPublicKey, charset, signType);
        } catch (AlipayApiException e)
        {
            throw TomatoMallException.alipaySignVerifyFailed();
        }
        if (!signVerified)
        {
            response.getWriter().print("fail"); // 签名验证失败，返回 fail
            return;
        }

        // 3. 处理业务逻辑（更新订单、减库存等）
        String tradeStatus = params.get("trade_status");
        if ("TRADE_SUCCESS".equals(tradeStatus))
        {
            String orderId = params.get("out_trade_no").split("#")[1]; // 您的订单号
            String alipayTradeNo = params.get("trade_no"); // 支付宝交易号
            String amount = params.get("total_amount"); // 支付金额

            // 更新订单状态（注意幂等性，防止重复处理）
            updateOrderStatus(orderId, alipayTradeNo, amount);

            // 扣减库存（建议加锁或乐观锁）
            productServiceImpl.reduceStock(orderId);
        }

        // 4. 必须返回纯文本的 "success"（支付宝要求）
        response.getWriter().print("success");
    }

    @Override
    public String deleteOrder(Integer orderId)
    {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(TomatoMallException::orderNotFound);
        productServiceImpl.refreshStockpile(orderId);
        order.setStringStatus("FAILED");
        orderRepository.save(order);
        return "删除成功";
    }

    private BigDecimal getTotalAmountByOrderId(Integer orderId)
    {
        Order order = orderRepository.findById(orderId).
                orElseThrow(TomatoMallException::orderNotFound);
        return order.getTotalAmount();
    }

    @Override
    public OrderController.RetPay pay(Integer orderId)
    {
        AlipayClient alipayClient = new DefaultAlipayClient(
                serverUrl,        // 支付宝网关地址（如：https://openapi.alipay.com/gateway.do）
                appId,            // 应用ID（从支付宝开放平台获取）
                privateKey,       // 应用私钥（用于签名）
                FORMAT,           // 数据格式（一般是JSON）
                charset,          // 字符编码（如UTF-8）
                alipayPublicKey,  // 支付宝公钥（验证签名用）
                signType          // 签名算法（如RSA2）
        );

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(notifyUrl);   // 异步通知地址（支付结果后台通知）
        request.setReturnUrl(returnUrl);   // 同步返回地址（支付成功后页面跳转）

        BigDecimal totalAmount = getTotalAmountByOrderId(orderId);
        JSONObject bizContent = new JSONObject();

        //可自定义订单号、订单标题
//        System.out.println("订单号=" + "" + System.currentTimeMillis() + "#" + orderId);
//        System.out.println("金额=" + "" + totalAmount);
//        System.out.println("subject=" + securityUtil.getCurrentAccount().getName() + "的支付订单#" + orderId);

        bizContent.put("out_trade_no", System.currentTimeMillis() + "#" + orderId);  // 商户订单号（需唯一）
        bizContent.put("total_amount", "" + totalAmount); // 订单金额（单位：元）
        bizContent.put("subject", securityUtil.getCurrentAccount().getName() + "的支付订单#" + orderId);       // 订单标题（商品描述）
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY"); // 固定产品码,即时到账交易
        request.setBizContent(bizContent.toString());

        String form;
        try
        {
            form = alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e)
        {
            throw TomatoMallException.alipayInnerError();
        }

        //自动完成前端页面跳转
//        httpResponse.setContentType("text/html;charset=" + charset);
//        httpResponse.getWriter().write(form); // 输出自动提交表单
//        httpResponse.getWriter().flush();
//        httpResponse.getWriter().close();

        OrderController.RetPay ret = new OrderController.RetPay();
        ret.setOrderId(String.valueOf(orderId));
        //for test
        System.out.println(form);

        ret.setPaymentForm(form);
        ret.setTotalAmount(totalAmount);
        ret.setPaymentMethod("Alipay");
        return ret;
    }

    private void updateOrderStatus(String SorderId, String SalipayTradeNo, String Samount)
    {
        Integer orderIdRet = Integer.valueOf(SorderId);
        BigDecimal amountRet = new BigDecimal(Samount);

        Order paidOrder = orderRepository.findById(orderIdRet)
                .orElseThrow(TomatoMallException::orderNotFound);
        if (paidOrder.getStringStatus().equals("PENDING"))
        {
            if (amountRet.equals(getTotalAmountByOrderId(orderIdRet)))
            {
                paidOrder.setStringStatus("SUCCESS");
                orderRepository.save(paidOrder);
                return;
            }
            throw TomatoMallException.alipayUnknownError();//支付金额不等，或者对非进行中的订单进行支付
        }
        //may wrong?
//        throw TomatoMallException.alipayUnknownError();//支付金额不等，或者对非进行中的订单进行支付
//可能有重复处理，暂时不报错
    }
}

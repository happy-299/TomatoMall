package com.seecoder.TomatoMall.vo;

import com.seecoder.TomatoMall.po.Order;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class OrderVO
{
    private Integer id;
    private Integer userId;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private Order.OrderStatus status;
    private LocalDateTime createTime;


    public Order toPO()
    {

        Order newOrder = new Order();
        newOrder.setId(this.id);
        newOrder.setUserId(this.userId);
        newOrder.setTotalAmount(this.totalAmount);
        newOrder.setPaymentMethod(this.paymentMethod);
        newOrder.setStatus(this.status);
        newOrder.setCreateTime(this.createTime);
        return newOrder;
    }
}


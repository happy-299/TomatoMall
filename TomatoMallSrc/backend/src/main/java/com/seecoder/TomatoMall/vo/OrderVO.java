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
    //优惠券
    private Boolean useCoupon;
    private Integer couponId;
    private BigDecimal beforeAmount;
    private BigDecimal reducedAmount;


    public Order toPO()
    {

        Order newOrder = new Order();
        newOrder.setId(this.id);
        newOrder.setUserId(this.userId);
        newOrder.setTotalAmount(this.totalAmount);
        newOrder.setPaymentMethod(this.paymentMethod);
        newOrder.setStatus(this.status);
        newOrder.setCreateTime(this.createTime);
        //
        newOrder.setUseCoupon(useCoupon);
        newOrder.setCouponId(couponId);
        newOrder.setBeforeAmount(beforeAmount);
        newOrder.setReducedAmount(reducedAmount);
        return newOrder;
    }
}


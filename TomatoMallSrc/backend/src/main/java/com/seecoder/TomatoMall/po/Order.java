package com.seecoder.TomatoMall.po;

import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.vo.OrderVO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orders")//注意避免sql关键字order
public class Order
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;//订单id

    @JoinColumn(name = "user_id", nullable = false,
            insertable = false, updatable = false)
    private Integer userId;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;//订单总金额;用了优惠券后的实际支付金额

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;//支付方式

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)//枚举类存储为字符串
    private OrderStatus status = OrderStatus.PENDING;//订单支付状态，默认PENDING

    @Column(name = "create_time")
    private LocalDateTime createTime;

    //增加优惠券部分
    @Basic
    private Boolean useCoupon = false;//是否使用了优惠券

    @JoinColumn(name = "coupon_id", nullable = false,
            insertable = false, updatable = false)
    private Integer couponId;//使用的优惠券id

    @Column(name = "before_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal beforeAmount;//用优惠券前，原来的订单总金额

    @Column(name = "reduced_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal reducedAmount;//用优惠券，扣减的金额

    @Column(name = "buy_tomato_cnt")
    private Integer buyTomatoCnt = 0;//如果是购买番茄的订单，需要填写此字段

//    @Column(name = "cartItemIds")
//    private List<String> cartItemIds;// 用于返还过期订单的冻结库存

    public enum OrderStatus
    {
        PENDING,   // 待支付
        SUCCESS,   // 支付成功
        FAILED,    // 支付失败
        TIMEOUT // 超时

    }

    public Order setStringStatus(String status)
    {
        if (status.equals("PENDING"))
        {
            this.status = OrderStatus.PENDING;
        } else if (status.equals("SUCCESS"))
        {
            this.status = OrderStatus.SUCCESS;
        } else if (status.equals("FAILED"))
        {
            this.status = OrderStatus.FAILED;
        } else if (status.equals("TIMEOUT"))
        {
            this.status = OrderStatus.TIMEOUT;
        } else
        {
            throw TomatoMallException.orderStatusInvalid();//订单状态无效
        }
        return this;
    }

    public String getStringStatus()
    {
        if (this.status == OrderStatus.FAILED)
        {
            return "FAILED";
        } else if (this.status == OrderStatus.TIMEOUT)
        {
            return "TIMEOUT";
        } else if (this.status == OrderStatus.PENDING)
        {
            return "PENDING";
        } else if (this.status == OrderStatus.SUCCESS)
        {
            return "SUCCESS";
        }
        return "ERROR_STATUS";
    }


    public OrderVO toVO()
    {
        OrderVO orderVO = new OrderVO();
        orderVO.setId(this.id);
        orderVO.setStatus(this.status);
        orderVO.setCreateTime(this.createTime);
        orderVO.setPaymentMethod(this.paymentMethod);
        orderVO.setTotalAmount(this.totalAmount);
        orderVO.setUserId(this.userId);
        //
        orderVO.setUseCoupon(this.useCoupon);
        orderVO.setCouponId(this.couponId);
        orderVO.setBeforeAmount(this.beforeAmount);
        orderVO.setReducedAmount(this.reducedAmount);
        return orderVO;
    }

}

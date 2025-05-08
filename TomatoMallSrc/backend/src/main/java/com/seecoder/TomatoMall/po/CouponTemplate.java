package com.seecoder.TomatoMall.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class CouponTemplate
{
    /*Coupon:id,title,description,img,type(FULL_REDUCTION),threshold（门槛）,
  reduce(less than threshold扣减金额),discount(0.00,1.00折扣，与门槛、扣减不兼容)，
    isInUse(bool,过期、使用、删除券置为false),
  expiryDateTime(LocalDateTime过期时间 "yyyy-MM-dd_HH-mm-ss")
  */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Basic
    private String title;

    @Basic
    private String description;

    @Basic
    private String img;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)//枚举类存储为字符串
    private CouponType type;

    public enum CouponType
    {
        FULL_REDUCTION   //满减
    }

    @Column(name = "threshold", nullable = false, precision = 10, scale = 2)
    private BigDecimal threshold;//满减门槛


    @Column(name = "reduce", nullable = false, precision = 10, scale = 2)
    private BigDecimal reduce;//满减金额

    @Column(name = "discount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discount;//折扣，与满减不兼容

    @Basic
    private boolean isInUse = true;//默认投入使用expiryDateTime

    @Basic
    private Integer restCnt;//剩余数量

    @Column(name = "expiry_date_time")
    private LocalDateTime expiryDateTime;


}

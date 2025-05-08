package com.seecoder.TomatoMall.vo;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponVO
{
    private Integer id;//coupon_id
    private Integer userId;//user_id
    private Integer couponTemplateId;//coupon_template_id
    private String title;
    private String description;
    private String img;

    private String type;//注意不一样！

    private BigDecimal threshold;//满减门槛
    private BigDecimal reduce;//满减金额
    private BigDecimal discount;//折扣，与满减不兼容
    private boolean inUse;//默认投入使用expiryDateTime
    private Integer restCnt;//剩余数量
    private String expiryDateTime;//yyyy-MM-dd_HH-mm-ss

}

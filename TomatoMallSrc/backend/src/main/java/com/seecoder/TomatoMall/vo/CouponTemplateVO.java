package com.seecoder.TomatoMall.vo;

import com.seecoder.TomatoMall.po.CouponTemplate;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CouponTemplateVO
{
    private Integer id;
    private String title;
    private String description;
    private String img;

    private String type;//注意不一样！

    private BigDecimal threshold;//满减门槛
    private BigDecimal reduce;//满减金额
    private BigDecimal discount;//折扣，与满减不兼容
    private Boolean inUse;//默认投入使用expiryDateTime
    private Integer restCnt;//剩余数量

    private String expiryDateTime;//yyyy-MM-dd_HH-mm-ss

}

package com.seecoder.TomatoMall.repository;


import com.seecoder.TomatoMall.po.Coupon;
import com.seecoder.TomatoMall.po.CouponTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;


public interface CouponTemplateRepository extends JpaRepository<CouponTemplate, Integer>
{
    List<CouponTemplate> findAll();

    List<CouponTemplate> findAllByInUseAndExpiryDateTimeBefore(boolean useStatus, LocalDateTime time);
}

package com.seecoder.TomatoMall.service;

import com.seecoder.TomatoMall.po.CouponTemplate;
import com.seecoder.TomatoMall.vo.CouponTemplateVO;
import com.seecoder.TomatoMall.vo.CouponVO;

import java.util.List;

public interface CouponService
{
    CouponTemplateVO createTemplate(CouponTemplateVO c);

    CouponVO create(Integer templateId);

    List<CouponVO> getAll();

    CouponVO getById(Integer couponId);

    List<CouponTemplateVO> getAllTemplates();

    CouponTemplateVO getTemplateById(Integer templateId);

    Boolean checkById(Integer templateId);
}

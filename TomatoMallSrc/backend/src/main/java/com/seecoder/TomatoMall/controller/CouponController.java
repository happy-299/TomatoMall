package com.seecoder.TomatoMall.controller;

import com.seecoder.TomatoMall.po.CouponTemplate;
import com.seecoder.TomatoMall.service.CouponService;
import com.seecoder.TomatoMall.service.ReviewService;
import com.seecoder.TomatoMall.vo.CouponTemplateVO;
import com.seecoder.TomatoMall.vo.CouponVO;
import com.seecoder.TomatoMall.vo.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
public class CouponController
{
    @Autowired
    CouponService couponService;

    // 创建一个优惠券模板
    @PostMapping("/template")
    public Response<CouponTemplateVO> createTemplate
    (@RequestBody CouponTemplateVO couponTemplateVO)
    {
        return Response.buildSuccess(couponService.createTemplate(couponTemplateVO));
    }

    //给当前用户发放couponTemplate->id的一张优惠券
    @PostMapping("/{templateId}")
    public Response<CouponVO> create(@PathVariable Integer templateId)
    {
        return Response.buildSuccess(couponService.create(templateId));
    }

    //查找当前用户所有优惠券
    @GetMapping("/all")
    public Response<List<CouponVO>> getAll()
    {
        return Response.buildSuccess(couponService.getAll());
    }

    //查找当前用户指定id的优惠券
    @GetMapping("/{couponId}")
    public Response<CouponVO> getById(@PathVariable Integer couponId)
    {
        return Response.buildSuccess(couponService.getById(couponId));
    }

    //检查用户是否持有templateId的券
    @GetMapping("/check/{templateId}")
    public Response<Boolean> checkById(@PathVariable Integer templateId)
    {
        return Response.buildSuccess(couponService.checkById(templateId));
    }

    //查找所有优惠券模板
    @GetMapping("/template/all")
    public Response<List<CouponTemplateVO>> getAllTemplates()
    {
        return Response.buildSuccess(couponService.getAllTemplates());
    }

    //查找优惠券模板->id
    @GetMapping("/template/{templateId}")
    public Response<CouponTemplateVO> getTemplateById(@PathVariable Integer templateId)
    {
        return Response.buildSuccess(couponService.getTemplateById(templateId));
    }
}

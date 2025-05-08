package com.seecoder.TomatoMall.serviceImpl;

import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.po.Account;
import com.seecoder.TomatoMall.po.Coupon;
import com.seecoder.TomatoMall.po.CouponTemplate;
import com.seecoder.TomatoMall.repository.CouponRepository;
import com.seecoder.TomatoMall.repository.CouponTemplateRepository;
import com.seecoder.TomatoMall.service.CouponService;
import com.seecoder.TomatoMall.util.SecurityUtil;
import com.seecoder.TomatoMall.vo.CouponTemplateVO;
import com.seecoder.TomatoMall.vo.CouponVO;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.stream.Collectors;

@Service
@EnableRetry
@Getter
@Setter
public class CouponServiceImpl implements CouponService
{

    @Autowired
    CouponRepository couponRepository;
    @Autowired
    CouponTemplateRepository couponTemplateRepository;
    @Autowired
    SecurityUtil securityUtil;
    @Autowired
    private UtilServiceImpl utilServiceImpl;


    private CouponVO toVO(Integer couponId)
    {
        Coupon c = couponRepository.findById(couponId)
                .orElseThrow(TomatoMallException::couponNotFound);
        CouponTemplate t = c.getCouponTemplate();

        return CouponVO.builder()
                .id(c.getId())
                .userId(c.getAccount().getId())
                .couponTemplateId(c.getCouponTemplate().getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .img(t.getImg())
                .type(t.getStringType())
                .threshold(t.getThreshold())
                .reduce(t.getReduce())
                .discount(t.getDiscount())
                .inUse(t.getInUse())
                .restCnt(t.getRestCnt())
                .expiryDateTime(time2str(t.getExpiryDateTime()))
                .build();
    }

    private CouponVO toVO(Coupon coupon)
    {
        return toVO(coupon.getId());
    }

    private LocalDateTime str2time(String str)
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        return LocalDateTime.parse(str, formatter);
    }

    private String time2str(LocalDateTime time)
    {
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
    }

    @Override
    @Transactional
    public CouponTemplateVO createTemplate(CouponTemplateVO c)
    {
        CouponTemplate t = new CouponTemplate();
        t.setTitle(c.getTitle());
        t.setDescription(c.getDescription());
        t.setImg(c.getImg());

        String Type = c.getType();
        t.setStringType(c.getType());//如果type非法会抛出异常
        if (Type.equals("FULL_REDUCTION"))
        {
            if (c.getThreshold().compareTo(c.getReduce()) <= 0)
            {
                throw TomatoMallException.couponThresholdInvaild();
            }
            t.setThreshold(c.getThreshold());
            t.setReduce(c.getReduce());
            t.setDiscount(new BigDecimal("0.00"));
        } else
        {
            //do nothing currently
        }

        t.setInUse(c.getInUse());
        t.setRestCnt(c.getRestCnt());
        t.setExpiryDateTime(str2time(c.getExpiryDateTime()));

        couponTemplateRepository.save(t);
        return t.toVO();//may wrong? id
    }

    @Override
    @Transactional
    public CouponVO create(Integer templateId)
    {
        Account acc = securityUtil.getCurrentAccount();
        CouponTemplate ct = couponTemplateRepository.findById(templateId)
                .orElseThrow(TomatoMallException::couponTemplateNotFound);
        int cnt = ct.getRestCnt();
        if (cnt <= 0)
        {
            throw TomatoMallException.couponUsedUp();
        }
        ct.setRestCnt(cnt - 1);//发一张少一张
        Coupon coupon = new Coupon();
        coupon.setCouponTemplate(ct);
        coupon.setAccount(acc);
        couponRepository.save(coupon);

        return this.toVO(coupon);
    }

    @Override
    @Transactional
    public List<CouponVO> getAll()
    {
        List<Coupon> list = couponRepository.findAllByAccount_Id(
                securityUtil.getCurrentAccount().getId()
        );
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CouponVO getById(Integer couponId)
    {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(TomatoMallException::couponNotFound);
        return this.toVO(coupon);
    }

    @Override
    @Transactional
    public List<CouponTemplateVO> getAllTemplates()
    {
        List<CouponTemplate> list = couponTemplateRepository.findAll();
        return list.stream().map(CouponTemplate::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CouponTemplateVO getTemplateById(Integer templateId)
    {
        CouponTemplate couponTemplate = couponTemplateRepository
                .findById(templateId)
                .orElseThrow(TomatoMallException::couponTemplateNotFound);
        return couponTemplate.toVO();
    }

    @Override
    @Transactional
    public Boolean checkById(Integer templateId)
    {
        return couponRepository.existsByCouponTemplate_IdAndAccount_Id
                (templateId, securityUtil.getCurrentAccount().getId());
    }

    //清理过期优惠券
    @Scheduled(fixedRate = 1000 * 15) // 每 15s 检查一次
    @Transactional
    public void removeExpiredCoupon()
    {
        //查询真正使用的过期优惠券
        List<CouponTemplate> expiredCouponTemplates =
                couponTemplateRepository.findAllByInUseAndExpiryDateTimeBefore(
                        true, LocalDateTime.now()
                );

        System.out.println("触发优惠券过期检查，expList.size=" + expiredCouponTemplates.size());

        //模板退出使用，删除所有用户的过期券
        expiredCouponTemplates.forEach(couponTemplate ->
        {
            couponTemplate.setInUse(false);
            couponRepository.deleteAllByCouponTemplate_Id(couponTemplate.getId());
        });
    }
}

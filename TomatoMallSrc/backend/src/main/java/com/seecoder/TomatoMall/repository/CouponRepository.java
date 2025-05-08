package com.seecoder.TomatoMall.repository;


import com.seecoder.TomatoMall.po.Coupon;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface CouponRepository extends JpaRepository<Coupon, Integer>
{
    List<Coupon> findAllByAccount_Id(Integer id);

    void deleteAllByCouponTemplate_Id(Integer id);

    Boolean existsByCouponTemplate_IdAndAccount_Id(Integer id, Integer uid);
}

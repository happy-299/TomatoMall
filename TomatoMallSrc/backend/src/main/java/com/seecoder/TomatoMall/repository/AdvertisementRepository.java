package com.seecoder.TomatoMall.repository;


import com.seecoder.TomatoMall.po.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Integer>
{
    @Modifying
    @Query("DELETE FROM Advertisement a WHERE a.productId = ?1")
    void deleteByProductId(Integer productId);  // 用于删除product的时候删除ad
}

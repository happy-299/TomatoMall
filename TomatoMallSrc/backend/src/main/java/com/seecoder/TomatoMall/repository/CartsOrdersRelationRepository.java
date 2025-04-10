package com.seecoder.TomatoMall.repository;


import com.seecoder.TomatoMall.po.CartsOrdersRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface CartsOrdersRelationRepository extends JpaRepository<CartsOrdersRelation, Integer>
{
    List<CartsOrdersRelation> findAllByOrderId(Integer orderId);
}

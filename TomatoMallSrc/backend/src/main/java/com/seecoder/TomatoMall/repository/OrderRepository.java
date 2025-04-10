package com.seecoder.TomatoMall.repository;


import com.seecoder.TomatoMall.po.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;


public interface OrderRepository extends JpaRepository<Order, Integer>
{

    List<Order> findOrdersByStatusAndCreateTimeBefore(Order.OrderStatus status, LocalDateTime time);
}

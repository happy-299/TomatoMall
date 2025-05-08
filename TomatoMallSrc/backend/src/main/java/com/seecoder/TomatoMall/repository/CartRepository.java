package com.seecoder.TomatoMall.repository;


import com.seecoder.TomatoMall.po.Cart;
import com.seecoder.TomatoMall.vo.CartVO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface CartRepository extends JpaRepository<Cart, Integer>
{
    Cart findCartByProductIdAndUserId(Integer productId, Integer UserId);

    Cart findCartById(Integer cartItemId);

    List<Cart> findAllByUserId(Integer userId);

    int deleteAllByUserId(Integer userId);

    int deleteAllByProductId(Integer productId);
}

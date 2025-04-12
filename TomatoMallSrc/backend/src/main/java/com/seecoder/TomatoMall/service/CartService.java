package com.seecoder.TomatoMall.service;


import com.seecoder.TomatoMall.controller.CartController;
import com.seecoder.TomatoMall.vo.CartVO;

import java.util.List;

public interface CartService
{
    CartController.RetAddProduct addProduct(CartVO cartVo);

    String deleteProduct(Integer cartItemId);

    String adjustProductQuantity(Integer cartItemId, Integer quantity);

    CartController.RetGetAllProducts getAllProducts();

    CartController.RetCheckout checkout(CartController.CheckoutRequest checkoutRequest);

    String deleteAllProduct();
}

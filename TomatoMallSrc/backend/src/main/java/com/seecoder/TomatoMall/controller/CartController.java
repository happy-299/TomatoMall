package com.seecoder.TomatoMall.controller;

import com.seecoder.TomatoMall.service.CartService;
import com.seecoder.TomatoMall.vo.CartVO;
import com.seecoder.TomatoMall.vo.Response;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;


@RestController
@RequestMapping("/api/cart")
public class CartController
{

    @Autowired
    CartService cartService;


    //    1.1 加入商品到购物车
    @PostMapping
    public Response<RetAddProduct> addProduct(@RequestBody CartVO cartVO)
    {
        RetAddProduct ret = cartService.addProduct(cartVO);
        return Response.buildSuccess(ret);
    }

    //    1.2 删除购物车商品
    @DeleteMapping("/{cartItemId}")
    public Response<String> deleteProduct(@PathVariable Integer cartItemId)
    {
        return Response.buildSuccess(cartService.deleteProduct(cartItemId));
    }

    //追加购物车内全部删除的方法
    @DeleteMapping
    public Response<String> deteleAllProduct()
    {
        return Response.buildSuccess(cartService.deleteAllProduct());
    }

    //    1.3 修改购物车商品数量
//    注意，不能超出库存数量
    @PatchMapping("/{cartItemId}")
    public Response<String> adjustProductQuantity(@PathVariable Integer cartItemId, @RequestBody AdjustBody ad)
    {
        return Response.buildSuccess(cartService.adjustProductQuantity(cartItemId, ad.getQuantity()));
    }

    //1.4 获取购物车商品列表
    @GetMapping
    public Response<RetGetAllProducts> getAllProducts()
    {
        return Response.buildSuccess(cartService.getAllProducts());
    }

    //2.1 提交订单
    @PostMapping("/checkout")
    public Response<RetCheckout> checkout(@RequestBody CheckoutRequest checkoutRequest)
    {
        return Response.buildSuccess(cartService.checkout(checkoutRequest));
    }

    @Data
    public static class CheckoutRequest
    {
        private List<String> cartItemIds;
        private ShippingAddress shipping_address;
        private String payment_method;

        @Data
        public static class ShippingAddress
        {
            private String recipientName;
            private String telephone;
            private String zipCode;
            private String location;
        }
    }

    @Data
    public static class RetCheckout
    {
        private String orderId;
        private String username;
        private BigDecimal totalAmount;
        private String paymentMethod = "ALIPAY";
        private String createTime;
        private String status = "PENDING";
    }


    @Data
    public static class RetAddProduct
    {
        private String cartItemId;
        private String productId;
        private String title;
        private BigDecimal price;
        private String description;
        private String cover;
        private String detail;
        private Integer quantity;

    }

    @Data
    public static class AdjustBody
    {
        private Integer quantity;
    }

    @Data
    public static class RetGetAllProducts
    {
        private List<RetAddProduct> items;
        private Integer total;//购物车一共多少种商品（同种商品只算一件）
        private BigDecimal totalAmount;//购物车商品总价

    }

}


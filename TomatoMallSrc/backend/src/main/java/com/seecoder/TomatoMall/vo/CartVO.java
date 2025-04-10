package com.seecoder.TomatoMall.vo;

import com.seecoder.TomatoMall.po.Cart;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CartVO
{
    private Integer id;
    private Integer userId;
    private Integer productId;
    private Integer quantity;


    public Cart toPO()
    {

        Cart newCart = new Cart();
        newCart.setId(this.id);
        newCart.setUserId(this.userId);
        newCart.setProductId(this.productId);
        newCart.setQuantity(this.quantity);
        return newCart;
    }
}


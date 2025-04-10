package com.seecoder.TomatoMall.vo;

import com.seecoder.TomatoMall.po.CartsOrdersRelation;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CartsOrdersRelationVO
{
    private Integer id;
    private Integer cartItemId;
    private Integer orderId;


    public CartsOrdersRelation toPO()
    {

        CartsOrdersRelation cor = new CartsOrdersRelation();
        cor.setId(this.id);
        cor.setOrderId(this.orderId);
        cor.setCartItemId(this.cartItemId);
        return cor;
    }
}


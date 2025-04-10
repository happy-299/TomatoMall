package com.seecoder.TomatoMall.po;

import com.seecoder.TomatoMall.vo.CartsOrdersRelationVO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import javax.persistence.*;


@Getter
@Setter
@NoArgsConstructor
@Entity
public class CartsOrdersRelation//已完成交易的记录->   只是用于记录关系，对于TIMEOUT订单需要回退库存使用
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;//


    @JoinColumn(name = "cartitem_id", nullable = false
            , insertable = false, updatable = false)
    private Integer cartItemId;

    @JoinColumn(name = "order_id", nullable = false
            , insertable = false, updatable = false)
    private Integer orderId;


    public CartsOrdersRelationVO toVO()
    {
        CartsOrdersRelationVO vo = new CartsOrdersRelationVO();
        vo.setId(this.id);
        vo.setCartItemId(this.cartItemId);
        vo.setOrderId(this.orderId);
        return vo;
    }

}

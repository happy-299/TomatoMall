package com.seecoder.TomatoMall.po;

import com.seecoder.TomatoMall.vo.CartVO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.seecoder.TomatoMall.util.SecurityUtil;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Cart
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;//cartItemId 购物车商品id

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "product_id", nullable = false)
    private Integer productId;


    @Column(nullable = false)
    private Integer quantity = 1;//商品数量,默认为1


    public CartVO toVO()
    {
        CartVO cartVO = new CartVO();
        cartVO.setId(this.id);
        cartVO.setUserId(this.userId);
        cartVO.setProductId(this.productId);
        cartVO.setQuantity(this.quantity);
        return cartVO;
    }

}

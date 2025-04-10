package com.seecoder.TomatoMall.po;

import com.seecoder.TomatoMall.vo.StockpileVO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Stockpile
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private Integer frozen;

    @OneToOne
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    @Version
    private Integer version;

    public Stockpile withProductId(Integer productId)
    {
        this.productId = productId;
        return this;
    }

    public Stockpile withAmount(Integer amount)
    {
        this.amount = amount;
        return this;
    }

    public Stockpile withFrozen(Integer frozen)
    {
        this.frozen = frozen;
        return this;
    }

    public StockpileVO toVO()
    {
        StockpileVO vo = new StockpileVO();
        vo.setId(this.id);
        vo.setAmount(this.amount);
        vo.setFrozen(this.frozen);
        vo.setProductId(this.productId);

        vo.setVersion(this.version);
        return vo;
    }

}

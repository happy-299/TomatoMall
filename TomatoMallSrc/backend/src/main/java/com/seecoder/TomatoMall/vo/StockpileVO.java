package com.seecoder.TomatoMall.vo;

import com.seecoder.TomatoMall.po.Stockpile;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StockpileVO
{
    private Integer id;
    private Integer amount;
    private Integer frozen;
    private Integer productId;

    private Integer version;

    public Stockpile toPO()
    {
        Stockpile po = new Stockpile();
        po.setId(this.id);
        po.setAmount(this.amount);
        po.setFrozen(this.frozen);
        po.setProductId(this.productId);

        po.setVersion(this.version);
        return po;
    }
}

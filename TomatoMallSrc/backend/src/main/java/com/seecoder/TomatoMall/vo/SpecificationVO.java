package com.seecoder.TomatoMall.vo;

import com.seecoder.TomatoMall.po.Specification;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SpecificationVO {
    private Integer id;
    private String item;
    private String value;
    private Integer productId;

    public Specification toPO() {
        Specification po = new Specification();
        po.setId(this.id);
        po.setItem(this.item);
        po.setValue(this.value);
        po.setProductId(productId);
        return po;
    }
}

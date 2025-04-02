package com.seecoder.TomatoMall.vo;

import com.seecoder.TomatoMall.po.Product;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProductVO {
    private Integer id;
    private String title;
    private BigDecimal price;
    private Double rate;
    private String description;
    private String cover;
    private String detail;
    private List<SpecificationVO> specifications;


    public Product toPO() {
        // attention: this will not set the specifications!!
        // you need to manually set that after using to PO
        Product po = new Product();
        po.setId(this.id);
        po.setTitle(this.title);
        po.setPrice(this.price);
        po.setRate(this.rate);
        po.setDescription(this.description);
        po.setCover(this.cover);
        po.setDetail(this.detail);
        return po;
    }
}


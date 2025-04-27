package com.seecoder.TomatoMall.po;

import com.seecoder.TomatoMall.vo.SpecificationVO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Specification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String item;

    @Column(nullable = false, length = 255)
    private String value;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @ManyToOne
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    public Specification withProductId(Integer productId) {
        this.productId = productId;
        return this;
    }



    public SpecificationVO toVO() {
        SpecificationVO vo = new SpecificationVO();
        vo.setId(this.id);
        vo.setItem(this.item);
        vo.setValue(this.value);
        return vo;
    }

}


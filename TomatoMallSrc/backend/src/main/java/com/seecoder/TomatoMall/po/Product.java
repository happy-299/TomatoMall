package com.seecoder.TomatoMall.po;

import com.seecoder.TomatoMall.vo.ProductVO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import javax.persistence.*;
import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Product
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Basic
    @Column(nullable = false, length = 50)
    private String title;

    @Basic
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Basic
    @Column(nullable = false)
    private Double rate;

    @Basic
    @Column(length = 2000)
    private String description;

    @Basic
    @Column(length = 500)
    private String cover;

    @Basic
    @Column(length = 2000)
    private String detail;

    @OneToMany(mappedBy = "product")
    private List<Specification> specifications;

    @OneToOne(mappedBy = "product")
    private Stockpile stockpile;

    public Product updateFromVO(ProductVO vo)
    {
        //todo may wrong! 当字段为空的时候不应该更新
//        this.title = vo.getTitle();
//        this.price = vo.getPrice();
//        this.rate = vo.getRate();
//        this.description = vo.getDescription();
//        this.cover = vo.getCover();
//        this.detail = vo.getDetail();
//        return this;

        return this;
    }


    public ProductVO toVO()
    {
        ProductVO vo = new ProductVO();
        vo.setId(this.id);
        vo.setTitle(this.title);
        vo.setPrice(this.price);
        vo.setRate(this.rate);
        vo.setDescription(this.description);
        vo.setCover(this.cover);
        vo.setDetail(this.detail);

        return vo;
    }


}

package com.seecoder.TomatoMall.po;

import com.seecoder.TomatoMall.vo.AdvertisementVO;
import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class Advertisement
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imgUrl;

    @Column(name = "product_id", nullable = false)
    private Integer productId;


    public AdvertisementVO toVO()
    {
        return AdvertisementVO
                .builder()
                .id(this.id)
                .content(this.content)
                .imgUrl(this.imgUrl)
                .productId(this.productId)
                .title(this.title)
                .build();
    }

}

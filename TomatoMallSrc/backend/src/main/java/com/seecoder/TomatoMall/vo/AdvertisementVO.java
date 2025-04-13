package com.seecoder.TomatoMall.vo;

import com.seecoder.TomatoMall.po.Advertisement;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementVO
{
    private Integer id;
    private String title;
    private String content;
    private String imgUrl;
    private Integer productId;


    public Advertisement toPO()
    {

        Advertisement ad = new Advertisement();
        ad.setContent(this.content);
        if (this.id != null) ad.setId(this.id);//createAd可能第一次id为null
        ad.setTitle(this.title);
        ad.setImgUrl(this.imgUrl);
        ad.setProductId(this.productId);
        return ad;
    }
}


package com.seecoder.TomatoMall.po;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
public class ReviewImg {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "review_id", nullable = false)
    private Integer reviewId;

    @Column(nullable = false)
    private String imgUrl;

    public ReviewImg(Integer reviewId, String imgUrl) {
        this.reviewId = reviewId;
        this.imgUrl = imgUrl;
    }

    public ReviewImg() {
    }
}

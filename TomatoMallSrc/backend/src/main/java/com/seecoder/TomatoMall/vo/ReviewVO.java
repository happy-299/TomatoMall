package com.seecoder.TomatoMall.vo;

import com.seecoder.TomatoMall.po.Review;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ReviewVO {
    private Integer id;
    private Integer productId;
    private Integer userId;
    private Double rating;
    private String content;
    private LocalDateTime createTime = LocalDateTime.now();
    private List<String> images;

    public Review toPO() {
        Review review = new Review();
        review.setId(id);
        review.setProductId(productId);
        review.setUserId(userId);
        review.setRating(rating);
        review.setContent(content);
        review.setCreateTime(createTime);
        review.setReviewImgs(images);
        return review;
    }
}

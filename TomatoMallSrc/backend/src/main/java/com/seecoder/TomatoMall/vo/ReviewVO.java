package com.seecoder.TomatoMall.vo;

import com.seecoder.TomatoMall.po.Review;
import com.seecoder.TomatoMall.po.ReviewImg;
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
    private LocalDateTime createTime;
    private List<String> images;

    public Review toPO() {
        Review review = new Review();
        review.setId(id);
        review.setProductId(productId);
        review.setUserId(userId);
        review.setRating(rating);
        review.setContent(content);
        review.setCreateTime(createTime);
        List<ReviewImg> imgs = new ArrayList<>();
        images.stream()
                .map(image -> new ReviewImg(id, image))
                .forEach(imgs::add);
        review.setImages(imgs);
        return review;
    }
}

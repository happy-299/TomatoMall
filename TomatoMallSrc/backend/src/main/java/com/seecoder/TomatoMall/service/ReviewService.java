package com.seecoder.TomatoMall.service;

import com.seecoder.TomatoMall.po.Review;
import com.seecoder.TomatoMall.vo.ReviewVO;
import org.springframework.data.domain.Page;

public interface ReviewService {
    Review addReview(ReviewVO request);
    Page<Review> getProductReviews(Integer productId, int page, int size);
    Double getProductAverageRating(Integer productId);
}

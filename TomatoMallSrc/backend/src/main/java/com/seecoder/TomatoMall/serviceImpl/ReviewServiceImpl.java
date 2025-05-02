package com.seecoder.TomatoMall.serviceImpl;

import com.seecoder.TomatoMall.po.Review;
import com.seecoder.TomatoMall.repository.ReviewImgRepository;
import com.seecoder.TomatoMall.repository.ReviewRepository;
import com.seecoder.TomatoMall.service.ReviewService;
import com.seecoder.TomatoMall.vo.ReviewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ReviewServiceImpl implements ReviewService {
    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewImgRepository imgRepository;

    @Override
    public Review addReview(ReviewVO request) {
        Review review = request.toPO();

        Review savedReview = reviewRepository.save(review);
        if (request.getImages() != null) {
            imgRepository.saveAll(review.getImages());
        }

        return savedReview;
    }

    public Page<Review> getProductReviews(Integer productId, int page, int size) {
        return reviewRepository.findByProductId(productId, PageRequest.of(page, size));
    }

    public Double getProductAverageRating(Integer productId) {
        return reviewRepository.calculateAverageRating(productId);
    }
}

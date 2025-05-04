package com.seecoder.TomatoMall.controller;

import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.po.Review;
import com.seecoder.TomatoMall.service.ProductService;
import com.seecoder.TomatoMall.service.ReviewService;
import com.seecoder.TomatoMall.vo.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;
    @Autowired
    private ReviewService reviewService;

    // 1. 获取商品列表
    @GetMapping
    public Response<List<ProductVO>> getAllProducts() {
        List<ProductVO> products = productService.getAllProducts();
        return Response.buildSuccess(products);
    }

    // 2. 获取指定商品
    @GetMapping("/{id}")
    public Response<ProductVO> getProductById(@PathVariable Integer id) {
        ProductVO product = productService.getProductById(id);
        return Response.buildSuccess(product);
    }

    // 3. 更新商品
    @PutMapping
    public Response<String> updateProduct(@RequestBody ProductVO productVO) {
        productService.updateProduct(productVO);
        return Response.buildSuccess("更新成功");
    }

    // 4. 创建商品
    @PostMapping
    public Response<ProductVO> createProduct(@RequestBody ProductVO productVO) {
        ProductVO createdProduct = productService.createProduct(productVO);
        return Response.buildSuccess(createdProduct);
    }

    // 5. 删除商品
    @DeleteMapping("/{id}")
    public Response<String> deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
        return Response.buildSuccess("删除成功");
    }

    // 6. 调整库存（需要单独的请求体DTO）
    @PatchMapping("/stockpile/{productId}")
    public Response<String> adjustStockpile(
            @PathVariable Integer productId,
            @RequestBody StockAdjustmentRequest request) {

        productService.adjustStockpile(
                productId,
                request.getAmount(),
                request.getFrozen()
        );
        return Response.buildSuccess("调整库存成功");
    }

    // 7. 查询库存
    @GetMapping("/stockpile/{productId}")
    public Response<StockpileVO> getStockpile(@PathVariable Integer productId) {
        StockpileVO stockpile = productService.getStockpile(productId);
        return Response.buildSuccess(stockpile);
    }

    /*
     * =================================
     * |            review             |
     * =================================
     */

    @PostMapping("/{productId}/reviews")
    public Response<Review> addReview(
            @PathVariable Integer productId,
            @RequestBody ReviewVO reviewVO) {
        // ensure the reviewVO has correct productId
        reviewVO.setProductId(productId);
        Review review = reviewService.addReview(reviewVO);
        return Response.buildSuccess(review);
    }

    @GetMapping("/{productId}/reviews")
    public Response<PageResponseVO<Review>> getReviews(
            @PathVariable Integer productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<Review> reviews = reviewService.getProductReviews(productId, page, size);
        return Response.buildSuccess(new PageResponseVO<>(reviews));
    }

    @GetMapping("/{productId}/rating")
    public Response<Double> getAverageRating(@PathVariable Integer productId) {
        Double rating = reviewService.getProductAverageRating(productId);
        return Response.buildSuccess(rating);
    }



    // 库存调整请求DTO
    @Getter
    @Setter
    @NoArgsConstructor
    static class StockAdjustmentRequest {
        private Integer amount;
        private Integer frozen;
    }
}


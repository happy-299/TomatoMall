package com.seecoder.TomatoMall.service;

import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.vo.ProductVO;
import com.seecoder.TomatoMall.vo.StockpileVO;
import java.util.List;

public interface ProductService {
    // 获取所有商品
    List<ProductVO> getAllProducts();

    // 根据ID获取商品
    ProductVO getProductById(Integer id) throws TomatoMallException;

    // 更新商品
    void updateProduct(ProductVO productVO) throws TomatoMallException;

    // 创建商品
    ProductVO createProduct(ProductVO productVO);

    // 删除商品
    void deleteProduct(Integer id) throws TomatoMallException;

    // 调整库存
    void adjustStockpile(Integer productId, Integer amount, Integer frozen) throws TomatoMallException;

    // 查询库存
    StockpileVO getStockpile(Integer productId) throws TomatoMallException;
}

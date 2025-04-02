package com.seecoder.TomatoMall.serviceImpl;

import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.po.*;
import com.seecoder.TomatoMall.repository.*;
import com.seecoder.TomatoMall.service.ProductService;
import com.seecoder.TomatoMall.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SpecificationRepository specificationRepository;

    @Autowired
    private StockpileRepository stockpileRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductVO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::convertToProductVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVO getProductById(Integer id) throws TomatoMallException {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> TomatoMallException.productNotFound());
        return convertToProductVO(product);
    }

    @Override
    @Transactional
    public void updateProduct(ProductVO productVO) throws TomatoMallException {
        Product product = productRepository.findById(productVO.getId())
                .orElseThrow(() -> TomatoMallException.productNotFound());

        // 更新商品基本信息
        product.updateFromVO(productVO);
        productRepository.save(product);

        // 全量更新规格信息
        specificationRepository.deleteByProductId(product.getId());
        if(productVO.getSpecifications() != null) {
            productVO.getSpecifications().stream()
                    .map(vo -> vo.toPO().withProductId(product.getId()))
                    .forEach(specificationRepository::save);
        }
    }

    @Override
    @Transactional
    public ProductVO createProduct(ProductVO productVO) {
        // 保存商品基本信息
        Product product = productRepository.save(productVO.toPO());

        // 保存规格信息
        if(productVO.getSpecifications() != null) {
            productVO.getSpecifications().stream()
                    .map(vo -> vo.toPO().withProductId(product.getId()))
                    .forEach(specificationRepository::save);
        }

        // 初始化库存
        stockpileRepository.save(new Stockpile()
                .withProductId(product.getId())
                .withAmount(0)
                .withFrozen(0));

        return convertToProductVO(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Integer id) throws TomatoMallException {
        if (!productRepository.existsById(id)) {
            throw TomatoMallException.productNotFound();
        }
        specificationRepository.deleteByProductId(id);
        stockpileRepository.deleteByProductId(id);
        productRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void adjustStockpile(Integer productId, Integer amount, Integer frozen) throws TomatoMallException {
        Stockpile stockpile = stockpileRepository.findByProductId(productId)
                .orElseThrow(() -> TomatoMallException.productNotFound());

        if (amount != null) stockpile.setAmount(amount);
        if (frozen != null) stockpile.setFrozen(frozen);
        stockpileRepository.save(stockpile);
    }

    @Override
    @Transactional(readOnly = true)
    public StockpileVO getStockpile(Integer productId) throws TomatoMallException {
        return stockpileRepository.findByProductId(productId)
                .orElseThrow(() -> TomatoMallException.productNotFound())
                .toVO();
    }

    // this util will automatically set the specification
    // if you need to convert to productVO, plz use this one
    private ProductVO convertToProductVO(Product product) {
        ProductVO vo = product.toVO();
        vo.setSpecifications(specificationRepository.findByProductId(product.getId())
                .stream()
                .map(Specification::toVO)
                .collect(Collectors.toList()));
        return vo;
    }
}

package com.seecoder.TomatoMall.serviceImpl;

import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.po.*;
import com.seecoder.TomatoMall.repository.*;
import com.seecoder.TomatoMall.service.ProductService;
import com.seecoder.TomatoMall.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@EnableRetry
public class ProductServiceImpl implements ProductService
{

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SpecificationRepository specificationRepository;

    @Autowired
    private StockpileRepository stockpileRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CartsOrdersRelationRepository cartsOrdersRelationRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private UtilServiceImpl utilServiceImpl;

    @Override
    @Transactional(readOnly = true)
    public List<ProductVO> getAllProducts()
    {
        return productRepository.findAll().stream()
                .map(this::convertToProductVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVO getProductById(Integer id) throws TomatoMallException
    {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> TomatoMallException.productNotFound());
        return convertToProductVO(product);
    }

    @Override
    @Transactional
    public void updateProduct(ProductVO productVO) throws TomatoMallException
    {
        Product product = productRepository.findById(productVO.getId())
                .orElseThrow(() -> TomatoMallException.productNotFound());

        // 更新商品基本信息
//        product.updateFromVO(productVO);
        utilServiceImpl.updateFromVO(product, productVO);//解决可能的bug，应该只更新非空字段

        productRepository.save(product);

        // 全量更新规格信息
        specificationRepository.deleteByProductId(product.getId());
        if (productVO.getSpecifications() != null)
        {
            productVO.getSpecifications().stream()
                    .map(vo -> vo.toPO().withProductId(product.getId()))
                    .forEach(specificationRepository::save);
        }
    }

    @Override
    @Transactional
    public ProductVO createProduct(ProductVO productVO)
    {
        // 保存商品基本信息
        Product product = productRepository.save(productVO.toPO());

        // 保存规格信息
        if (productVO.getSpecifications() != null)
        {
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
    public void deleteProduct(Integer id) throws TomatoMallException
    {
        if (!productRepository.existsById(id))
        {
            throw TomatoMallException.productNotFound();
        }
        specificationRepository.deleteByProductId(id);
        stockpileRepository.deleteByProductId(id);
        productRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void adjustStockpile(Integer productId, Integer amount, Integer frozen) throws TomatoMallException
    {
        Stockpile stockpile = stockpileRepository.findByProductId(productId)
                .orElseThrow(() -> TomatoMallException.productNotFound());

        if (amount != null) stockpile.setAmount(amount);
        if (frozen != null) stockpile.setFrozen(frozen);
        stockpileRepository.save(stockpile);
    }

    @Override
    @Transactional(readOnly = true)
    public StockpileVO getStockpile(Integer productId) throws TomatoMallException
    {
        return stockpileRepository.findByProductId(productId)
                .orElseThrow(() -> TomatoMallException.productNotFound())
                .toVO();
    }

    // this util will automatically set the specification
    // if you need to convert to productVO, plz use this one
    private ProductVO convertToProductVO(Product product)
    {
        ProductVO vo = product.toVO();
        vo.setSpecifications(specificationRepository.findByProductId(product.getId())
                .stream()
                .map(Specification::toVO)
                .collect(Collectors.toList()));
        return vo;
    }

    @Scheduled(fixedRate = 1000 * 15) // 每 15s 检查一次
    @Transactional
    public void releaseLockedStockpile()
    {
        // 1. 查询超时未支付订单
        int waitingMin =30;//保留订单多少分钟
        List<Order> expiredOrders = orderRepository.findOrdersByStatusAndCreateTimeBefore(
                Order.OrderStatus.PENDING, LocalDateTime.now().minusMinutes(waitingMin)
        );

        System.out.println("触发冻结库存检查，expList.size=" + expiredOrders.size());

        // 2. 恢复库存
        expiredOrders.forEach(order ->
        {
            Integer orderId = order.getId();
            refreshStockpile(orderId);

            // 3. 更新订单状态
            order.setStringStatus("TIMEOUT");
            orderRepository.save(order);
        });
    }

    @Transactional
    public void refreshStockpile(Integer orderId)
    {
        List<CartsOrdersRelation> corList = cartsOrdersRelationRepository.findAllByOrderId(orderId);

        corList.forEach(cor ->
        {
            Integer cartItemId = cor.getCartItemId();
            Cart cart = cartRepository.findCartById(cartItemId);
            StockpileVO stockpileVO = getStockpile(cart.getProductId());
            adjustStockpile(cart.getProductId(),
                    stockpileVO.getAmount() + cart.getQuantity(),
                    stockpileVO.getFrozen() - cart.getQuantity());//完成恢复
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void reduceStock(String SorderId)
    {
        Integer orderId = Integer.valueOf(SorderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(TomatoMallException::orderNotFound);
        if (!order.getStringStatus().equals("SUCCESS"))//订单未成功不要减少库存
        {
            throw TomatoMallException.reduceStockpileFailed();
        }
        List<CartsOrdersRelation> corList = cartsOrdersRelationRepository.findAllByOrderId(orderId);
        corList.forEach(cor ->
        {
            try
            {// 每个购物车项独立重试
                reduceStockForCartItem(cor);
            } catch (TomatoMallException ex)
            {
                if ("409".equals(ex.getErrCode()))
                { // 仅处理乐观锁冲突
                    throw ex; // 触发重试
                } else
                {
                    throw TomatoMallException.reduceStockpileFailed();
                }
            }
        });

    }

    // 为单个购物车项的库存扣减添加重试
    @Retryable(
            value = TomatoMallException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, maxDelay = 1000)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 每次重试使用新事务
    public void reduceStockForCartItem(CartsOrdersRelation cor)
    {
        Integer cartItemId = cor.getCartItemId();
        Cart cart = cartRepository.findCartById(cartItemId);
        Integer quantity = cart.getQuantity();
        Integer productId = cart.getProductId();
        Stockpile stockpile = getStockpile(productId).toPO();

        if (quantity > stockpile.getFrozen())
        {
            throw TomatoMallException.reduceStockpileFailed();//不可重试异常
        }

        stockpile.setFrozen(stockpile.getFrozen() - quantity);
        try
        {
            stockpileRepository.save(stockpile);
        } catch (ObjectOptimisticLockingFailureException ex)
        {
            // 转换为可重试异常
            throw TomatoMallException.optimisticLockConflict();
        }
    }
}

package com.seecoder.TomatoMall.serviceImpl;


import com.seecoder.TomatoMall.controller.CartController;
import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.po.Cart;

import com.seecoder.TomatoMall.po.CartsOrdersRelation;
import com.seecoder.TomatoMall.po.Order;
import com.seecoder.TomatoMall.repository.CartRepository;
import com.seecoder.TomatoMall.repository.CartsOrdersRelationRepository;
import com.seecoder.TomatoMall.repository.OrderRepository;
import com.seecoder.TomatoMall.service.CartService;
import com.seecoder.TomatoMall.util.OssUtil;
import com.seecoder.TomatoMall.util.SecurityUtil;
import com.seecoder.TomatoMall.vo.CartVO;
import com.seecoder.TomatoMall.vo.ProductVO;
import com.seecoder.TomatoMall.vo.StockpileVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartServiceImpl implements CartService
{
    @Autowired
    SecurityUtil securityUtil;

    @Autowired
    ProductServiceImpl productService;

    @Autowired
    CartRepository cartRepository;

    @Autowired
    CartsOrdersRelationRepository cartsOrdersRelationRepository;

    @Autowired
    private OrderRepository orderRepository;

    private void setAllForRetAddProduct(Integer productId, Integer userId, CartController.RetAddProduct ret)
    {
        //填写返回data
        Cart savedCart = cartRepository.findCartByProductIdAndUserId(productId, userId);
        if (savedCart == null)
        {
            throw TomatoMallException.cartItemNotExists();
        }
        ret.setCartItemId(String.valueOf(savedCart.getId()));
        ret.setProductId(String.valueOf(productId));
        ProductVO productVo = productService.getProductById(productId);
        ret.setTitle(productVo.getTitle());
        ret.setPrice(productVo.getPrice());
        ret.setDescription(productVo.getDescription());
        ret.setCover(productVo.getCover());
        ret.setDetail(productVo.getDetail());
        ret.setQuantity(savedCart.getQuantity());
    }


    @Override
    @Transactional
    public CartController.RetAddProduct addProduct(CartVO cartVo)
    {
        Integer userId = securityUtil.getCurrentAccount().getId();
        Integer productId = cartVo.getProductId();
        if (cartVo.getQuantity() <= 0)
        {
            throw TomatoMallException.quantityInvalid();
        }
        if (cartRepository.findCartByProductIdAndUserId(productId, userId) != null)
        {
            throw TomatoMallException.cartItemExists();
        }

        cartVo.setUserId(userId);
        Cart cart = cartVo.toPO();
        cartRepository.save(cart);//保存

        //填写返回data
        CartController.RetAddProduct ret = new CartController.RetAddProduct();
        setAllForRetAddProduct(productId, userId, ret);
        return ret;
    }

    @Override
    @Transactional//事务提交
    public String deleteProduct(Integer cartItemId)
    {
        System.out.println("cartItemId=" + cartItemId);
        if (cartRepository.findCartById(cartItemId) == null)
        {
            throw TomatoMallException.cartItemNotExists();
        }
        cartRepository.deleteById(cartItemId);
        return "删除成功";
    }

    @Override
    @Transactional
    public String adjustProductQuantity(Integer cartItemId, Integer quantity)
    {
        Cart cart = cartRepository.findCartById(cartItemId);
        if (cart == null)
        {
            throw TomatoMallException.cartItemNotExists();
        }
        StockpileVO stockpileVo = productService.getStockpile(cart.getProductId());
//        int availableAmount = stockpileVo.getAmount() - stockpileVo.getFrozen();
        if (quantity <= 0)
        {
            throw TomatoMallException.quantityInvalid();
        }
        if (quantity > stockpileVo.getAmount())//注意不可超出库存数！
        {
            throw TomatoMallException.quantityMoreThanAmount();
        }
        //更改库存数并保存
        cart.setQuantity(quantity);
        cartRepository.save(cart);
        return "修改数量成功";

    }

    @Override
    @Transactional
    public CartController.RetGetAllProducts getAllProducts()
    {
        Integer userId = securityUtil.getCurrentAccount().getId();
        Integer total = 0;//商品种类数
        BigDecimal totalAmount = new BigDecimal("0.00");// 总价
        CartController.RetGetAllProducts ret = new CartController.RetGetAllProducts();
        List<CartController.RetAddProduct> items = new ArrayList<>();
        List<Cart> cartList = cartRepository.findAllByUserId(userId);

        total = cartList.size();
        for (int i = 0; i < total; i++)
        {
            CartController.RetAddProduct oneItem = new CartController.RetAddProduct();
            Integer productId = cartList.get(i).getProductId();
            setAllForRetAddProduct(productId, userId, oneItem);
            items.add(oneItem);

            BigDecimal amountOfThisItem = new BigDecimal(String.valueOf(oneItem.getPrice()));
            amountOfThisItem = amountOfThisItem.multiply(BigDecimal.valueOf(oneItem.getQuantity()));
            totalAmount = totalAmount.add(amountOfThisItem);
        }

        ret.setTotal(total);
        ret.setTotalAmount(totalAmount);
        ret.setItems(items);
        return ret;
    }

    @Override
    @Transactional
    public CartController.RetCheckout checkout(CartController.CheckoutRequest checkoutRequest)
    {
        List<Cart> cartItems = new ArrayList<>();
        List<String> cartItemIds = checkoutRequest.getCartItemIds();
        int total = cartItemIds.size();
        BigDecimal totalAmount = new BigDecimal("0.00");
        for (int i = 0; i < total; i++)
        {
            //验证购物车商品及库存,计算总价
            Cart cart = cartRepository.findCartById(Integer.valueOf(cartItemIds.get(i)));
            cartItems.add(cart);
            if (cart == null)//校验商品存在
            {
                throw TomatoMallException.cartItemNotExists();
            }
            StockpileVO stockpileVo = productService.getStockpile(cart.getProductId());
            if (cart.getQuantity() > stockpileVo.getAmount())//校验库存
            {
                throw TomatoMallException.quantityMoreThanAmount();
            }
            //锁定库存
            productService.adjustStockpile(cart.getProductId(),
                    stockpileVo.getAmount() - cart.getQuantity(),
                    stockpileVo.getFrozen() + cart.getQuantity());

            //计算总价
            BigDecimal amountOfThisItem = new BigDecimal(String.valueOf(
                    productService.getProductById(cart.getProductId()).getPrice()));
            amountOfThisItem = amountOfThisItem.multiply(BigDecimal.valueOf(cart.getQuantity()));
            totalAmount = totalAmount.add(amountOfThisItem);


        }

        //创建订单
        Order order = new Order();
        order.setUserId(securityUtil.getCurrentAccount().getId());
        order.setPaymentMethod(checkoutRequest.getPayment_method());
        order.setCreateTime(LocalDateTime.now());
        order.setTotalAmount(totalAmount);
        order.setStringStatus("PENDING");
        Integer orderId = orderRepository.save(order).getId();

        //每一项都添加到CartsOrdersRelation中，便于回退超时订单的冻结库存
        cartItems.forEach(cart ->
        {
            CartsOrdersRelation cor = new CartsOrdersRelation();
            cor.setCartItemId(cart.getId());
            cor.setOrderId(orderId);
            cartsOrdersRelationRepository.save(cor);
        });

        //todo 是否需要清理购物车？

        //填写返回表单
        CartController.RetCheckout ret = new CartController.RetCheckout();
        ret.setUsername(securityUtil.getCurrentAccount().getUsername());
        ret.setOrderId(String.valueOf(orderId));
        ret.setStatus(order.getStringStatus());
        ret.setCreateTime(String.valueOf(order.getCreateTime()));
        ret.setTotalAmount(order.getTotalAmount());
        ret.setPaymentMethod(order.getPaymentMethod());

        return ret;

    }
}

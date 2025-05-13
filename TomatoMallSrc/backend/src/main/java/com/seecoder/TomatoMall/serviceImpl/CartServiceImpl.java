package com.seecoder.TomatoMall.serviceImpl;


import com.seecoder.TomatoMall.controller.CartController;
import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.po.Cart;

import com.seecoder.TomatoMall.po.CartsOrdersRelation;
import com.seecoder.TomatoMall.po.Coupon;
import com.seecoder.TomatoMall.po.Order;
import com.seecoder.TomatoMall.repository.CartRepository;
import com.seecoder.TomatoMall.repository.CartsOrdersRelationRepository;
import com.seecoder.TomatoMall.repository.CouponRepository;
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
    @Autowired
    private CouponRepository couponRepository;

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
    public String deleteAllProduct()
    {
        Integer userId = securityUtil.getCurrentAccount().getId();
        int deletedRow = cartRepository.deleteAllByUserId(userId);
        if (deletedRow > 0)
            return "成功删除购物车中所有商品";
        return "购物车已为空";
    }

    @Override
    public String deleteAllByProductId(Integer id)
    {
        int row = cartRepository.deleteAllByProductId(id);
        return "完成删除:" + row + "项";
    }

    @Override
    public CartController.RetCheckout buyTomato(CartController.CheckoutRequest checkoutRequest)
    {
        final int RATE = 10;//1 yuan -> 10 tomato
        int cnt = checkoutRequest.getTomato();
        if (cnt <= 0)
        {
            throw TomatoMallException.tomatoIllegal();
        }

        BigDecimal totalAmount = new BigDecimal("1.00")
                .multiply(new BigDecimal(cnt)).divide(new BigDecimal(RATE));

        Order order = new Order();
        fillOrder(checkoutRequest.getUseCoupon(), order,
                checkoutRequest.getCouponId(), totalAmount, checkoutRequest.getPayment_method());
        order.setBuyTomatoCnt(cnt);//注意设置，最后需要支付成功后增加tomato数量
        return fillRetWithOrder(order);
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
        int couponId = checkoutRequest.getCouponId();
        Boolean useCoupon = checkoutRequest.getUseCoupon();
        String paymentMethod = checkoutRequest.getPayment_method();
        //--->> 增加优惠券部分
        //如果使用优惠券，验证优惠券合法性，并扣减金额
        int orderId = fillOrder(useCoupon, order, couponId, totalAmount, paymentMethod);

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


        return fillRetWithOrder(order);

    }

    @Transactional
    public Integer fillOrder(Boolean useCoupon, Order order,
                             Integer couponId, BigDecimal totalAmount, String paymentMethod)
    {
        if (useCoupon)
        {
            Coupon coupon = couponRepository.findById(couponId)
                    .orElseThrow(TomatoMallException::couponNotFound);

            if (coupon.getCouponTemplate().getStringType().equals("FULL_REDUCTION"))
            {
                //满减类型

                //检查是否达到门槛
                if (totalAmount.compareTo(coupon.getCouponTemplate().getThreshold()) < 0)
                {
                    throw TomatoMallException.couponThresholdNotReach();
                }
                //执行金额扣减并填写字段
                order.setBeforeAmount(totalAmount);
                order.setReducedAmount(coupon.getCouponTemplate().getReduce());
                order.setTotalAmount(order.getBeforeAmount().subtract(order.getReducedAmount()));
                //删除优惠券
                couponRepository.deleteById(couponId);
            } else
            {
                //目前无其他类型优惠券
                System.out.println("WRONG:目前没有其他优惠券类型");
            }
        } else
        {
            //未使用优惠券
            order.setTotalAmount(totalAmount);
            order.setReducedAmount(new BigDecimal("0.00"));
            order.setBeforeAmount(totalAmount);
        }

        order.setUserId(securityUtil.getCurrentAccount().getId());
        order.setPaymentMethod(paymentMethod);
        order.setCreateTime(LocalDateTime.now());
//        order.setTotalAmount(totalAmount);//由上面完成
        order.setStringStatus("PENDING");
        Integer orderId = orderRepository.save(order).getId();

        return orderId;
    }

    public CartController.RetCheckout fillRetWithOrder(Order order)
    {
        CartController.RetCheckout ret = new CartController.RetCheckout();
        ret.setUsername(securityUtil.getCurrentAccount().getUsername());
        ret.setOrderId(String.valueOf(order.getId()));
        ret.setStatus(order.getStringStatus());
        ret.setCreateTime(String.valueOf(order.getCreateTime()));
        ret.setTotalAmount(order.getTotalAmount());
        ret.setPaymentMethod(order.getPaymentMethod());
        //优惠券
        ret.setBeforeAmount(order.getBeforeAmount());
        ret.setReducedAmount(order.getReducedAmount());
        ret.setUseCoupon(order.getUseCoupon());
        return ret;
    }


}

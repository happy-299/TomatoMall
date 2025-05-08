<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter, useRoute } from 'vue-router';
import CartCard from '../../components/CartCard.vue';
import {
  getCart,
  updateCartItemQuantity,
  deleteAllCartItems,
  deleteCartItem,
  type CartItem
} from '../../api/cart';
import { getStockpile } from '../../api/product';
import { submitOrder } from '../../api/order';
import { getUserCoupons } from '../../api/coupon';
import type { UserCoupon } from '../../types/coupon';

const router = useRouter();
const route = useRoute();

// 响应式数据
const cartItems = ref<CartItem[]>([]);
const selectedItems = ref<string[]>([]);
const selectAll = ref(false);
const loading = ref(false);
const stockpiles = ref<Record<string, number>>({});
const userCoupons = ref<UserCoupon[]>([]);
const selectedCoupon = ref<UserCoupon | null>(null);
const showCouponDialog = ref(false);
const shippingAddress = ref({
  recipientName: '',
  telephone: '',
  zipCode: '',
  location: ''
})

// 计算属性
const totalAmount = computed(() => {
  return cartItems.value
      .filter(item => selectedItems.value.includes(item.cartItemId))
      .reduce((sum, item) => sum + item.price * item.quantity, 0);
});

const finalAmount = computed(() => {
  if (!selectedCoupon.value) return totalAmount.value;
  if (totalAmount.value >= selectedCoupon.value.threshold) {
    return totalAmount.value - selectedCoupon.value.reduce;
  }
  return totalAmount.value;
});

watch(selectedItems, (newVal) => {
  selectAll.value = newVal.length === cartItems.value.length && cartItems.value.length > 0;
});

const fetchCart = async () => {
  try {
    loading.value = true;
    const res = await getCart();
    cartItems.value = res.data.data.items || [];

    // 初始化库存对象
    stockpiles.value = {};
    
    // 修改库存获取逻辑
    await Promise.all(cartItems.value.map(async item => {
      try {
        const stockRes = await getStockpile(item.productId);
        // 确保设置默认值为0
        stockpiles.value[item.productId] = stockRes.data.data?.amount ?? 0;
      } catch (error) {
        console.error(`获取商品 ${item.productId} 库存失败:`, error);
        stockpiles.value[item.productId] = 0;
      }
    }));

    const highlightProductId = route.query.highlight as string;
    if (highlightProductId) {
      const highlightedItem = cartItems.value.find(item => item.productId === highlightProductId);
      if (highlightedItem) {
        selectedItems.value = [highlightedItem.cartItemId];
      }
    } else {
      selectedItems.value = [];
    }
  } catch (error) {
    ElMessage.error('获取购物车失败');
  } finally {
    loading.value = false;
  }
};

const handleSelectAll = () => {
  selectedItems.value = selectAll.value
      ? cartItems.value.map(item => item.cartItemId)
      : [];
};

const handleQuantityChange = async ({ item, type }: { item: CartItem; type: 'add' | 'subtract' }) => {
  try {
    let newQuantity = item.quantity;
    const stock = stockpiles.value[item.productId];

    if (type === 'add' && newQuantity < stock) {
      newQuantity++;
    } else if (type === 'subtract' && newQuantity > 1) {
      newQuantity--;
    } else {
      return;
    }

    await updateCartItemQuantity(item.cartItemId, newQuantity);
    item.quantity = newQuantity;
  } catch (error) {
    ElMessage.error('数量更新失败');
  }
};

const handleClearCart = async () => {
  try {
    await ElMessageBox.confirm(
        '确定要清空整个购物车吗？该操作不可恢复！',
        '警告',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
          center: true
        }
    );

    await deleteAllCartItems();
    await fetchCart();
    ElMessage.success('购物车已清空');
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('清空失败');
    }
  }
};

const handleDelete = async (cartItemId: string) => {
  try {
    await deleteCartItem(cartItemId);
    cartItems.value = cartItems.value.filter(item => item.cartItemId !== cartItemId);
    selectedItems.value = selectedItems.value.filter(id => id !== cartItemId);
    ElMessage.success('删除成功');
  } catch (error) {
    ElMessage.error('删除失败');
  }
};

const fetchUserCoupons = async () => {
  try {
    const res = await getUserCoupons();
    console.log('获取优惠券响应:', res);
    
    if (res.data.code === '200') {
      userCoupons.value = res.data.data.filter(coupon => {
        // 检查优惠券是否在使用中
        if (!coupon.inUse) {
          console.log('优惠券未启用:', coupon);
          return false;
        }
        
        // 处理日期格式
        const [datePart, timePart] = coupon.expiryDateTime.split('_');
        const formattedDate = `${datePart}T${timePart.replace(/-/g, ':')}`;
        const expiryDate = new Date(formattedDate);
        const now = new Date();
        
        // 检查是否过期
        const isValid = expiryDate > now;
        if (!isValid) {
          console.log('优惠券已过期:', coupon, '过期时间:', expiryDate, '当前时间:', now);
        }
        return isValid;
      });
      
      console.log('过滤后的可用优惠券:', userCoupons.value);
    }
  } catch (error) {
    console.error('获取优惠券失败:', error);
    ElMessage.error('获取优惠券失败');
  }
};

const handleSelectCoupon = (coupon: UserCoupon) => {
  console.log('选择优惠券:', coupon);
  console.log('当前订单金额:', totalAmount.value);
  
  if (totalAmount.value >= coupon.threshold) {
    selectedCoupon.value = coupon;
    showCouponDialog.value = false;
    ElMessage.success('优惠券选择成功');
  } else {
    ElMessage.warning(`订单金额未达到优惠券使用门槛（满${coupon.threshold}元）`);
  }
};

const handleRemoveCoupon = () => {
  selectedCoupon.value = null;
  ElMessage.success('优惠券已移除');
};

const handleCheckout = async () => {
  try {
    if (!shippingAddress.value.recipientName) {
      ElMessage.warning('请输入收货人姓名');
      return;
    }
    if (!shippingAddress.value.telephone) {
      ElMessage.warning('请输入联系电话');
      return;
    }
    if (!shippingAddress.value.location) {
      ElMessage.warning('请输入收货地址');
      return;
    }

    // 添加调试日志
    console.log('准备提交订单，当前数据：', {
      selectedItems: selectedItems.value,
      shippingAddress: shippingAddress.value,
      selectedCoupon: selectedCoupon.value,
      totalAmount: totalAmount.value,
      finalAmount: finalAmount.value
    });

    const orderData = {
      cartItemIds: selectedItems.value,
      shipping_address: {
        recipientName: shippingAddress.value.recipientName,
        telephone: shippingAddress.value.telephone,
        zipCode: shippingAddress.value.zipCode,
        location: shippingAddress.value.location
      },
      payment_method: 'ALIPAY',
      useCoupon: !!selectedCoupon.value,
      couponId: selectedCoupon.value?.id || -1
    };

    // 添加调试日志
    console.log('提交订单数据：', orderData);

    const order = await submitOrder(orderData);

    // 添加调试日志
    console.log('订单提交响应：', order);

    if (order.data.code === '200') {
      await router.push({
        path: '/pay',
        query: {
          orderId: order.data.data.orderId.toString(),
          amount: finalAmount.value.toFixed(2),
          _t: Date.now().toString(36)
        }
      });
    } else {
      ElMessage.error(order.data.message || '订单创建失败');
    }

  } catch (error: any) {
    console.error('订单创建失败，详细错误：', {
      error,
      response: error.response?.data,
      status: error.response?.status
    });
    ElMessage.error(error.response?.data?.message || '订单创建失败，请稍后重试');
  }
};

onMounted(() => {
  fetchCart();
  fetchUserCoupons();
});
</script>

<template>
  <div class="cart-container">
    <h1 class="header">我的购物车</h1>

    <el-card class="cart-list" v-loading="loading">
      <div class="select-all">
        <div class="select-group">
          <el-checkbox
              v-model="selectAll"
              @change="handleSelectAll"
              :indeterminate="selectedItems.length > 0 && !selectAll"
          >
            全选（{{ selectedItems.length }}）
          </el-checkbox>
          <el-button
              type="danger"
              size="small"
              :disabled="!cartItems.length"
              @click="handleClearCart"
          >
            清空购物车
          </el-button>
        </div>
      </div>

      <el-checkbox-group v-model="selectedItems">
        <template v-if="!loading">
          <CartCard
              v-for="item in cartItems"
              :key="item.cartItemId"
              :item="item"
              :stock="stockpiles[item.productId] || 0"
              :highlighted="route.query.highlight === item.productId"
              @quantity-change="handleQuantityChange"
              @delete="handleDelete"
              @view-product="(productId) => router.push(`/product/${productId}`)"
          />
        </template>
      </el-checkbox-group>

      <div v-if="!cartItems.length" class="empty-cart">
        <span>购物车空空如也，快去选购商品吧~</span>
      </div>

      <el-card class="shipping-form" v-if="selectedItems.length > 0">
        <h2 class="form-title">收货信息</h2>
        <el-form label-width="100px">
          <el-form-item label="收货人">
            <el-input v-model="shippingAddress.recipientName" />
          </el-form-item>

          <el-form-item label="联系电话">
            <el-input v-model="shippingAddress.telephone" />
          </el-form-item>

          <el-form-item label="所在地区">
            <el-input v-model="shippingAddress.location" placeholder="省 市 区 详细地址（用空格分隔）" />
          </el-form-item>

          <el-form-item label="邮政编码">
            <el-input v-model="shippingAddress.zipCode" />
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="coupon-section" v-if="selectedItems.length > 0">
        <div class="coupon-header">
          <h2 class="form-title">优惠券</h2>
          <el-button 
            type="primary" 
            link 
            @click="showCouponDialog = true"
          >
            {{ selectedCoupon ? '更换优惠券' : '选择优惠券' }}
          </el-button>
        </div>
        
        <div v-if="selectedCoupon" class="selected-coupon">
          <div class="coupon-info">
            <span class="coupon-title">{{ selectedCoupon.title }}</span>
            <span class="coupon-desc">满{{ selectedCoupon.threshold }}减{{ selectedCoupon.reduce }}</span>
          </div>
          <el-button type="danger" link @click="handleRemoveCoupon">
            移除
          </el-button>
        </div>
      </el-card>

      <div class="checkout-bar">
        <div class="total-amount">
          <div>商品总额：¥{{ totalAmount.toFixed(2) }}</div>
          <div v-if="selectedCoupon" class="discount-amount">
            优惠金额：-¥{{ (totalAmount - finalAmount).toFixed(2) }}
          </div>
          <div class="final-amount">
            应付金额：<span class="amount">¥{{ finalAmount.toFixed(2) }}</span>
          </div>
        </div>
        <el-button
          type="primary"
          :disabled="!selectedItems.length"
          @click="handleCheckout"
        >
          去支付（{{ selectedItems.length }}件）
        </el-button>
      </div>
    </el-card>

    <el-dialog
      v-model="showCouponDialog"
      title="选择优惠券"
      width="500px"
    >
      <div class="coupon-list">
        <div
          v-for="coupon in userCoupons"
          :key="coupon.id"
          class="coupon-item"
          :class="{ 'disabled': totalAmount < coupon.threshold }"
          @click="handleSelectCoupon(coupon)"
        >
          <div class="coupon-content">
            <div class="coupon-info">
              <h3>{{ coupon.title }}</h3>
              <p>{{ coupon.description }}</p>
              <div class="coupon-details">
                <span class="threshold">满{{ coupon.threshold }}元可用</span>
                <span class="reduce">减{{ coupon.reduce }}元</span>
                <span class="expiry">有效期至: {{ coupon.expiryDateTime }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-if="!userCoupons.length" class="no-coupon">
          暂无可用优惠券
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.cart-container {
  padding: 24px;
  background: linear-gradient(120deg, #f0f9ff 0%, #e6f7ff 100%);
  min-height: 100vh;
}

.header {
  color: #2c3e50;
  margin-bottom: 24px;
  padding-left: 20px;
  font-size: 24px;
}

.cart-list {
  margin: 0 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.shipping-form {
  margin-top: 30px;
}

.form-title {
  color: #2c3e50;
  margin-bottom: 24px;
  padding-left: 20px;
  font-size: 20px;
}

.select-all {
  padding: 16px 24px;
  border-bottom: 1px solid #ebeef5;
}

.select-group {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.checkout-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: #f8fafc;
  border-top: 1px solid #ebeef5;
}

.checkout-bar .total-amount {
  font-size: 18px;
  color: #303133;
}

.checkout-bar .total-amount .amount {
  color: #f56c6c;
  font-size: 24px;
  font-weight: 700;
}

.checkout-bar .el-button {
  padding: 12px 36px;
  font-size: 16px;
  border-radius: 24px;
}

.empty-cart {
  padding: 40px 0;
  text-align: center;
  color: #909399;
  font-size: 16px;
}

.empty-cart span {
  display: inline-block;
  margin-top: 16px;
}

.coupon-section {
  margin-top: 20px;
}

.coupon-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.selected-coupon {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #f0f9ff;
  border-radius: 8px;
  border: 1px solid #409eff;
  margin-top: 12px;
}

.coupon-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.coupon-title {
  font-size: 16px;
  font-weight: 500;
  color: #2c3e50;
}

.coupon-desc {
  font-size: 14px;
  color: #409eff;
}

.discount-amount {
  color: #67c23a;
  font-size: 14px;
  margin: 4px 0;
}

.final-amount {
  font-size: 16px;
  color: #303133;
  margin-top: 4px;
}

.final-amount .amount {
  color: #f56c6c;
  font-size: 24px;
  font-weight: 700;
}

.coupon-list {
  max-height: 400px;
  overflow-y: auto;
}

.coupon-item {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s;
  background: #fff;
}

.coupon-item:hover:not(.disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}

.coupon-item.disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: #f5f7fa;
}

.coupon-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 8px;
  font-size: 14px;
}

.coupon-details .threshold {
  color: #409eff;
  font-weight: 500;
}

.coupon-details .reduce {
  color: #f56c6c;
  font-weight: 500;
}

.coupon-details .expiry {
  color: #909399;
}

.no-coupon {
  text-align: center;
  color: #909399;
  padding: 20px;
}
</style>
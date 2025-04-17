<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter, useRoute } from 'vue-router';
import {
  getCart,
  updateCartItemQuantity,
  deleteAllCartItems,
  deleteCartItem,
  type CartItem
} from '../../api/cart';
import { getStockpile } from '../../api/product';
import { submitOrder } from '../../api/order';

const router = useRouter();
const route = useRoute(); // Add this to access route parameters

// 响应式数据
const cartItems = ref<CartItem[]>([]);
const selectedItems = ref<string[]>([]);
const selectAll = ref(false);
const loading = ref(false);
const stockpiles = ref<Record<string, number>>({});
const shippingAddress = ref({
  recipientName: '',
  telephone: '',
  zipCode: '',
  location: '' // 合并的地址字符串
})

// 计算属性
const totalAmount = computed(() => {
  return cartItems.value
      .filter(item => selectedItems.value.includes(item.cartItemId))
      .reduce((sum, item) => sum + item.price * item.quantity, 0);
});

// 监听 selectAll 变化和 selectedItems 变化
watch(selectedItems, (newVal) => {
  selectAll.value = newVal.length === cartItems.value.length && cartItems.value.length > 0;
});

// 获取购物车数据
const fetchCart = async () => {
  try {
    loading.value = true;
    const res = await getCart();
    cartItems.value = res.data.data.items || [];

    await Promise.all(cartItems.value.map(async item => {
      const stockRes = await getStockpile(item.productId);
      stockpiles.value[item.productId] = stockRes.data.data?.amount || 0;
    }));

    // 检查是否有高亮商品ID (来自立即购买)
    const highlightProductId = route.query.highlight as string;
    if (highlightProductId) {
      // 找到对应的购物车项
      const highlightedItem = cartItems.value.find(item => item.productId === highlightProductId);
      if (highlightedItem) {
        // 选中这个商品
        selectedItems.value = [highlightedItem.cartItemId];
      }
    } else {
      // 没有高亮项时清空选择
      selectedItems.value = [];
    }
  } catch (error) {
    ElMessage.error('获取购物车失败');
  } finally {
    loading.value = false;
  }
};

// 全选处理
const handleSelectAll = () => {
  selectedItems.value = selectAll.value
      ? cartItems.value.map(item => item.cartItemId)
      : [];
};

// 数量调整
const handleQuantityChange = async (item: CartItem, type: 'add' | 'subtract') => {
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

// 清空购物车
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

// 删除单个商品
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

// 提交订单
const handleCheckout = async () => {
  try {
    // 调用接口
    const order = await submitOrder({
      cartItemIds: selectedItems.value,
      shipping_address: {
        recipientName: shippingAddress.value.recipientName,
        telephone: shippingAddress.value.telephone,
        zipCode: shippingAddress.value.zipCode,
        location: shippingAddress.value.location
      },
      payment_method: 'ALIPAY'
    });

    // 跳转支付
    await router.push({
      path: '/pay',
      query: {
        orderId: order.orderId.toString(),
        amount: order.totalAmount.toFixed(2),
        _t: Date.now().toString(36)
      }
    });

  } catch (error: any) {
    ElMessage.error(error.message || '订单创建失败');
  }
};

onMounted(() => {
  fetchCart();
});
</script>

<template>
  <div class="cart-container">
    <h1 class="header">我的购物车</h1>

    <el-card class="cart-list" v-loading="loading">
      <!-- 全选区域 -->
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

      <!-- 商品列表 -->
      <el-checkbox-group v-model="selectedItems">
        <div
            class="cart-item"
            v-for="item in cartItems"
            :key="item.cartItemId"
            :class="{ '': route.query.highlight === item.productId }"
        >
          <div class="item-left">
            <el-checkbox
                :label="item.cartItemId"
                class="item-check"
            />
            <img
                :src="item.cover || 'https://via.placeholder.com/100'"
                class="product-cover"
                @click="router.push(`/product/${item.productId}`)"
            />
          </div>

          <div class="item-info" @click="router.push(`/product/${item.productId}`)">
            <h3 class="title">{{ item.title }}</h3>
            <p class="description">{{ item.description }}</p>
            <div class="price-stock">
              <span class="price">¥{{ item.price.toFixed(2) }}</span>
              <span class="stock">库存: {{ stockpiles[item.productId] }}</span>
            </div>
          </div>

          <div class="item-actions">
            <div class="quantity-control">
              <el-button
                  circle
                  size="small"
                  :disabled="item.quantity <= 1"
                  @click.stop="handleQuantityChange(item, 'subtract')"
              >
                -
              </el-button>
              <span class="quantity">{{ item.quantity }}</span>
              <el-button
                  circle
                  size="small"
                  :disabled="item.quantity >= stockpiles[item.productId]"
                  @click.stop="handleQuantityChange(item, 'add')"
              >
                +
              </el-button>
            </div>
            <el-button
                type="danger"
                size="small"
                @click.stop="handleDelete(item.cartItemId)"
            >
              删除
            </el-button>
          </div>
        </div>
      </el-checkbox-group>

      <!-- 空状态 -->
      <div v-if="!cartItems.length" class="empty-cart">
        <span>购物车空空如也，快去选购商品吧~</span>
      </div>

      <!-- 收货地址表单 -->
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

      <!-- 结算栏 -->
      <div class="checkout-bar">
        <div class="total-amount">
          总计：<span class="amount">¥{{ totalAmount.toFixed(2) }}</span>
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
  </div>
</template>

<style scoped>
/* 原有样式保持不变 */
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
  color: #2c3e50 ;
  margin-bottom: 24px;
  padding-left: 20px;
  font-size: 20px;
}

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

.select-all {
  padding: 16px 24px;
  border-bottom: 1px solid #ebeef5;
}

.select-group {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.el-checkbox-group {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px 24px;
}

.cart-item {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 8px;
  background: #ffffff;
  transition: all 0.3s ease;
  border-bottom: 1px solid #ebeef5;
}

.cart-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

/* 高亮显示从"立即购买"跳转过来的商品 */
.highlighted-item {
  background: #f0f9ff;
  border: 1px solid #409eff;
  box-shadow: 0 0 8px rgba(64, 158, 255, 0.2);
}

.item-left {
  display: flex;
  align-items: center;
  min-width: 180px;
}

.item-check {
  margin-right: 20px;
}

.product-cover {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.2s;
}

.product-cover:hover {
  transform: scale(1.05);
}

.item-info {
  flex: 1;
  margin-left: 24px;
  cursor: pointer;
}

.title {
  color: #1a1a1a;
  margin-bottom: 8px;
  font-size: 18px;
}

.description {
  color: #666;
  font-size: 14px;
  margin-bottom: 12px;
  line-height: 1.5;
}

.price-stock {
  display: flex;
  align-items: center;
  margin-top: 30px;
  gap: 24px;
}

.price {
  color: #f56c6c;
  font-size: 18px;
  font-weight: 600;
}

.stock {
  color: #409eff;
  font-size: 14px;
}

.item-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  min-width: 140px;
}

.quantity-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quantity-control .el-button {
  width: 32px;
  height: 32px;
  font-size: 14px;
}

.quantity-control .quantity {
  min-width: 40px;
  text-align: center;
  font-weight: 500;
  color: #333;  /* 增加颜色对比度 */
  font-size: 14px;
  padding: 0 8px;  /* 添加内边距 */
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
</style>
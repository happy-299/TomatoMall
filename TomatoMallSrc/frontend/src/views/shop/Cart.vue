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

const router = useRouter();
const route = useRoute();

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
  location: ''
})

// 计算属性
const totalAmount = computed(() => {
  return cartItems.value
      .filter(item => selectedItems.value.includes(item.cartItemId))
      .reduce((sum, item) => sum + item.price * item.quantity, 0);
});

watch(selectedItems, (newVal) => {
  selectAll.value = newVal.length === cartItems.value.length && cartItems.value.length > 0;
});

const fetchCart = async () => {
  try {
    loading.value = true;
    const res = await getCart();
    cartItems.value = res.data.data.items || [];

    await Promise.all(cartItems.value.map(async item => {
      const stockRes = await getStockpile(item.productId);
      stockpiles.value[item.productId] = stockRes.data.data?.amount || 0;
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

const handleCheckout = async () => {
  try {
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
        <CartCard
            v-for="item in cartItems"
            :key="item.cartItemId"
            :item="item"
            :stock="stockpiles[item.productId]"
            :highlighted="route.query.highlight === item.productId"
            @quantity-change="handleQuantityChange"
            @delete="handleDelete"
            @view-product="(productId) => router.push(`/product/${productId}`)"
        />
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
</style>
<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElCard, ElButton, ElCheckbox } from 'element-plus'
import { getCart, updateCartItemQuantity, deleteCartItem, type CartItem } from '../../api/cart'
import { getStockpile } from '../../api/product'
import { useRouter } from 'vue-router'

const router = useRouter()
const cartItems = ref<CartItem[]>([])
const selectedItems = ref<string[]>([])
const selectAll = ref(false)
const loading = ref(false)
const stockpiles = ref<Record<string, number>>({})

// 获取购物车数据
const fetchCart = async () => {
  try {
    loading.value = true
    const res = await getCart()
    cartItems.value = res.data.data.items || []

    // 获取每个商品的库存
    await Promise.all(cartItems.value.map(async item => {
      const stockRes = await getStockpile(item.productId)
      stockpiles.value[item.productId] = stockRes.data.data?.amount || 0
    }))
  } catch (error) {
    ElMessage.error('获取购物车失败')
  } finally {
    loading.value = false
  }
}

// 处理数量变化
const handleQuantityChange = async (item: CartItem, type: 'add' | 'subtract') => {
  try {
    let newQuantity = item.quantity
    const stock = stockpiles.value[item.productId]

    if (type === 'add' && newQuantity < stock) {
      newQuantity++
    } else if (type === 'subtract' && newQuantity > 1) {
      newQuantity--
    }

    await updateCartItemQuantity(item.cartItemId, newQuantity)
    item.quantity = newQuantity
  } catch (error) {
    ElMessage.error('数量更新失败')
  }
}

// 删除商品
const handleDelete = async (cartItemId: string) => {
  try {
    await deleteCartItem(cartItemId)
    cartItems.value = cartItems.value.filter(item => item.cartItemId !== cartItemId)
    ElMessage.success('删除成功')
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 全选处理
const handleSelectAll = () => {
  selectedItems.value = selectAll.value
      ? cartItems.value.map(item => item.cartItemId)
      : []
}

// 计算总金额
const totalAmount = computed(() => {
  return cartItems.value
      .filter(item => selectedItems.value.includes(item.cartItemId))
      .reduce((sum, item) => sum + item.price * item.quantity, 0)
})

onMounted(() => {
  fetchCart()
})
</script>

<template>
  <div class="cart-container">
    <h1 class="header">我的购物车</h1>

    <el-card class="cart-list" v-loading="loading">
      <!-- 全选 -->
      <div class="select-all">
        <el-checkbox v-model="selectAll" @change="handleSelectAll">
          全选（{{ selectedItems.length }}）
        </el-checkbox>
      </div>

      <!-- 购物车商品列表 -->
      <div class="cart-item" v-for="item in cartItems" :key="item.cartItemId">
        <div class="item-left">
          <el-checkbox
              v-model="selectedItems"
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

      <!-- 空状态 -->
      <div v-if="!cartItems.length" class="empty-cart">
        <span>购物车空空如也，快去选购商品吧~</span>
      </div>

      <!-- 结算栏 -->
      <div class="checkout-bar">
        <div class="total-amount">
          总计：<span class="amount">¥{{ totalAmount.toFixed(2) }}</span>
        </div>
        <el-button
            type="primary"
            :disabled="!selectedItems.length"
            @click="router.push('/pay')"
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
  background: linear-gradient(120deg, #e3f6f5 0%, #d0eeff 100%);
  min-height: 100vh;
}

.header {
  color: #272643;
  margin-bottom: 24px;
  padding-left: 20px;
}

.cart-list {
  margin: 0 20px;
  border-radius: 8px;
}

.select-all {
  padding: 16px;
  border-bottom: 1px solid #eee;
}

.cart-item {
  display: flex;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
  transition: all 0.3s;
}

.cart-item:hover {
  background: #f8f9fa;
  transform: translateX(5px);
}

.item-left {
  display: flex;
  align-items: center;
  min-width: 150px;
}

.item-check {
  margin-right: 15px;
}

.product-cover {
  width: 100px;
  height: 100px;
  border-radius: 6px;
  cursor: pointer;
  object-fit: cover;
}

.item-info {
  flex: 1;
  margin: 0 20px;
  cursor: pointer;
}

.title {
  color: #272643;
  margin-bottom: 8px;
}

.description {
  color: #909399;
  font-size: 14px;
  margin-bottom: 12px;
}

.price-stock {
  display: flex;
  align-items: center;
  gap: 20px;
}

.price {
  color: #ff4d4f;
  font-size: 18px;
  font-weight: bold;
}

.stock {
  color: #2c698d;
  font-size: 14px;
}

.item-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  min-width: 120px;
}

.quantity-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quantity {
  min-width: 24px;
  text-align: center;
  color: #2c698d;
}

.empty-cart {
  padding: 40px 0;
  text-align: center;
  color: #909399;
}

.checkout-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 20px;
  gap: 30px;
}

.total-amount {
  font-size: 18px;
  color: #272643;
}

.amount {
  color: #ff4d4f;
  font-size: 24px;
  font-weight: bold;
}

:deep(.el-checkbox) {
  --el-checkbox-checked-text-color: #2c698d;
  --el-checkbox-checked-bg-color: #bae8e8;
  --el-checkbox-checked-input-border-color: #2c698d;
}
</style>
<script setup lang="ts">
import { ShoppingCart } from '@element-plus/icons-vue'
import { ElCard, ElButton, ElRate } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  updateAdvertisement,
  createAdvertisement,
  deleteAdvertisement,
  getAdvertisements
} from '../api/advertisement'

const router = useRouter()

const props = defineProps({
  product: {
    type: Object,
    required: true
  },
  stockpile: {
    type: Object,
    default: () => ({ amount: 0, frozen: 0 })
  },
  isAdmin: Boolean,
  cartItems: {
    type: Object,
    default: () => ({})
  },
  hasAdvertisement: Boolean
})

const emit = defineEmits([
  'delete',
  'ad-click',
  'stock-update',
  'cart-add',
  'cart-subtract',
  'edit-ad'
])

const handleCardClick = () => {
  router.push(`/product/${props.product.id}`)
}

const handleAdminAction = (event: Event, action: string, payload?: any) => {
  event.stopPropagation()
  if (action === 'delete') emit('delete', props.product.id)
  if (action === 'ad') emit('ad-click', props.product.id)
  if (action === 'stock') emit('stock-update', props.product)
}

const handleCartAction = (event: Event, type: 'add' | 'subtract') => {
  event.stopPropagation()
  emit(type === 'add' ? 'cart-add' : 'cart-subtract', props.product.id)
}

const handleBuyNow = (event: Event) => {
  event.stopPropagation()
  router.push({
    path: '/cart',
    query: {
      highlight: props.product.id
    }
  })
}
</script>

<template>
  <el-card
      class="product-card"
      @click="handleCardClick"
  >
    <img v-if="product.cover" :src="product.cover" class="product-cover" />
    <div class="product-info">
      <h3 class="title">{{ product.title }}</h3>

      <div class="price-rate">
        <div class="price-section">
          <span class="price">¥{{ product.price.toFixed(2) }}</span>
          <div class="stock-info">
            <span>库存: {{ stockpile.amount || 0 }}</span>
            <span class="frozen">(冻结: {{ stockpile.frozen || 0 }})</span>
          </div>
        </div>
        <el-rate
            :model-value="product.rate"
            :max="10"
            disabled
            :colors="['#272643', '#272643', '#272643']"
        />
      </div>

      <div v-if="isAdmin" class="admin-actions">
        <div class="action-group">
          <el-button
              v-if="hasAdvertisement"
              size="small"
              type="warning"
              @click.stop="$emit('edit-ad', product.id)"
          >
            更新广告
          </el-button>
          <el-button
              size="small"
              :type="hasAdvertisement ? 'danger' : 'primary'"
              @click.stop="$emit('ad-click', product.id)"
          >
            {{ hasAdvertisement ? '移除广告' : '添加广告' }}
          </el-button>
        </div>

        <div class="action-group">
          <el-button
              size="small"
              type="primary"
              @click.stop="$emit('stock-update', product)"
              color="#bae8e8"
          >
            库存管理
          </el-button>
          <el-button
              type="danger"
              size="small"
              @click.stop="$emit('delete', product.id)"
          >
            删除商品
          </el-button>
        </div>
      </div>

      <div class="user-actions">
        <el-button
            type="primary"
            class="buy-btn"
            @click.stop="handleBuyNow"
        >
          立即购买
        </el-button>

        <div class="cart-operations">
          <template v-if="cartItems[product.id]?.quantity > 0">
            <el-button
                circle
                size="small"
                @click.stop="$emit('cart-subtract', product.id)"
            >
              -
            </el-button>
            <span class="quantity">{{ cartItems[product.id]?.quantity }}</span>
            <el-button
                circle
                size="small"
                :disabled="cartItems[product.id]?.quantity >= stockpile.amount"
                @click.stop="$emit('cart-add', product.id)"
            >
              +
            </el-button>
          </template>
          <el-button
              v-else
              :icon="ShoppingCart"
              circle
              type="info"
              size="small"
              @click.stop="$emit('cart-add', product.id)"
          />
        </div>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.product-card {
  cursor: pointer;
  transition: transform 0.3s;
  border-radius: 8px;
}

.product-card:hover {
  transform: translateY(-5px);
}

.product-cover {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 8px 8px 0 0;
}

.product-info {
  padding: 12px;
}

.title {
  color: #272643;
  margin: 8px 0;
  height: 44px;
  overflow: hidden;
}

.price-rate {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.price-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  color: #ff4d4f;
  font-size: 18px;
  font-weight: bold;
}

.stock-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  font-size: 12px;
}

.frozen {
  color: #909399;
}

.admin-actions {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #f8f9fa;
  padding: 12px;
  border-radius: 6px;
  position: relative;
  z-index: 2;
}

.action-group {
  display: flex;
  gap: 8px;
  justify-content: space-between;
}

.action-group + .action-group {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid #eee;
}

.user-actions {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.buy-btn {
  background: #2c698d;
  border-color: #2c698d;
  color: white;
  transition: all 0.3s;
}

.buy-btn:hover {
  opacity: 0.9;
  transform: translateY(-2px);
}

.cart-operations {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quantity {
  min-width: 24px;
  text-align: center;
  color: #2c698d;
  font-weight: 500;
}

:deep(.el-button.is-circle) {
  width: 28px;
  height: 28px;
  padding: 0;
}
</style>
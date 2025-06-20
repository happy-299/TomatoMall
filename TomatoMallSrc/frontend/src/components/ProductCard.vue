<script setup lang="ts">
import { ShoppingCart } from '@element-plus/icons-vue'
import { ElCard, ElButton, ElRate, ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { addToCart } from '../api/cart'

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
  'edit-ad',
  'cart-updated'
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

  // 获取当前购物车数量和库存
  const currentQuantity = props.cartItems[props.product.id]?.quantity || 0
  const currentStock = props.stockpile.amount || 0

  // 处理增加操作时的库存验证
  if (type === 'add') {
    // 如果库存为0或当前数量已超过库存，直接返回
    if (currentStock <= 0 || currentQuantity >= currentStock) {
      return
    }
  }

  emit(type === 'add' ? 'cart-add' : 'cart-subtract', props.product.id)
}

const handleBuyNow = async (event: Event) => {
  event.stopPropagation();
  if (!props.product) return;

  // 检查库存
  if (props.stockpile.amount <= 0) {
    ElMessage.warning('商品库存不足');
    return;
  }

  try {
    const currentQuantity = props.cartItems[props.product.id]?.quantity || 0;

    // 如果购物车中没有该商品或数量为0，则添加一个
    if (currentQuantity === 0) {
      await addToCart(props.product.id, 1);
      emit('cart-updated'); // 触发父组件更新购物车
    }

    // 跳转到购物车页面并高亮该商品
    router.push({
      path: '/cart',
      query: {
        highlight: props.product.id,
        t: Date.now() // 强制刷新确保数据最新
      }
    });
  } catch (error) {
    ElMessage.error('操作失败，请重试');
  }
};
</script>

<template>
  <el-card
      class="product-card"
      @click="handleCardClick"
  >
    <img v-if="product.cover" :src="product.cover" class="product-cover"/>
    <div class="overlay"></div>
    <div class="product-info">
      <div class="info-main">
        <h3 class="title">{{ product.title }}</h3>
        <div class="price-rate">
          <div class="price-section">
            <span class="price">¥{{ product.price.toFixed(2) }}</span>
            <div class="stock-info">
              <span>库存: {{ stockpile.amount || 0 }}</span>
              <span class="frozen">(冻结: {{ stockpile.frozen || 0 }})</span>
            </div>
          </div>
        </div>
      </div>
      <div class="info-actions">
        <div v-if="isAdmin" class="admin-actions">
          <div class="action-group">
            <el-button
                v-if="hasAdvertisement"
                size="small"
                @click.stop="$emit('edit-ad', product.id)"
            >
              更新广告
            </el-button>
            <el-button
                size="small"
                @click.stop="$emit('ad-click', product.id)"
            >
              {{ hasAdvertisement ? '移除广告' : '添加广告' }}
            </el-button>
          </div>

          <div class="action-group">
            <el-button
                size="small"
                @click.stop="$emit('stock-update', product)"
            >
              库存管理
            </el-button>
            <el-button
                size="small"
                @click.stop="$emit('delete', product.id)"
            >
              删除商品
            </el-button>
          </div>
        </div>

        <div class="user-actions">
          <el-button
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
                  @click.stop="handleCartAction($event, 'subtract')"
              >
                -
              </el-button>
              <span class="quantity">{{
                  Math.min(cartItems[product.id]?.quantity, stockpile.amount)
                }}</span>
              <el-button
                  circle
                  size="small"
                  :disabled="cartItems[product.id]?.quantity >= stockpile.amount || stockpile.amount <= 0"
                  @click.stop="handleCartAction($event, 'add')"
              >
                +
              </el-button>
            </template>
            <el-button
                v-else
                :icon="ShoppingCart"
                circle
                size="small"
                :disabled="stockpile.amount <= 0"
                @click.stop="handleCartAction($event, 'add')"
            />
          </div>
        </div>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.product-card {
  cursor: pointer;
  transition: all 0.3s;
  border-radius: 16px;
  overflow: hidden;
  background: white;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: none;
  position: relative;
  height: 520px;
}

:deep(.el-card__body) {
  padding: 0;
  height: 100%;
  justify-content: flex-end;
}

.product-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 30px rgba(217, 83, 79, 0.15);
}

.product-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
  position: absolute;
  top: 0;
  left: 0;
  z-index: 1;
}

.product-card:hover .product-cover {
  transform: scale(1.05);
}

.overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.85) 0%, rgba(0, 0, 0, 0.2) 50%, transparent 100%);
  z-index: 2;
  transition: background 0.3s;
}

.product-info {
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
  z-index: 3;
  color: white;
  padding: 16px 16px 10px 16px;
  box-sizing: border-box;
}

.info-main {
  flex: 1 1 auto;
  overflow: hidden;
  margin-bottom: 8px;
}

.info-actions {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.title {
  color: #fff;
  margin-bottom: 6px;
  height: auto;
  max-height: 54px;
  overflow: hidden;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.5;
  text-shadow: 1px 1px 3px rgba(0, 0, 0, 0.7);
}

.price-rate {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.price-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  text-shadow: 1px 1px 3px rgba(0, 0, 0, 0.5);
}

.stock-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  font-size: 13px;
  color: #ddd;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.5);
}

.frozen {
  color: #bbb;
}

.admin-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(5px);
  padding: 8px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.action-group {
  display: flex;
  gap: 8px;
  justify-content: space-between;
}

.action-group + .action-group {
  margin-top: 4px;
  padding-top: 4px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.el-button {
  background: transparent !important;
  color: white !important;
  border: none !important;
  box-shadow: none !important;
  padding: 10px 20px !important;
  font-weight: 500;
  border-radius: 8px !important;
}

.el-button:hover {
  background: rgba(255, 255, 255, 0.2) !important;
}

.user-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cart-operations {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quantity {
  min-width: 28px;
  text-align: center;
  color: #fff;
  font-weight: 600;
  font-size: 14px;
}

:deep(.el-button.is-circle) {
  width: 32px;
  height: 32px;
  padding: 0 !important;
  background-color: transparent;
  color: white;
}

:deep(.el-button.is-circle:hover) {
  transform: scale(1.1);
  background-color: rgba(255, 255, 255, 0.2);
}

:deep(.el-button.is-circle.is-disabled) {
  background-color: transparent !important;
  color: rgba(255, 255, 255, 0.5) !important;
}

:deep(.el-button:focus),
:deep(.el-button:active) {
  outline: none !important;
  border: none !important;
  background-color: transparent !important;
}

:deep(.el-button:active) {
  border: none !important;
}
</style>
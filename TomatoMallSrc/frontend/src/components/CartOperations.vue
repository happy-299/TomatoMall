<template>
  <div class="cart-operations">
    <el-popover
        v-if="localQuantity > 0"
        placement="top"
        :width="160"
        trigger="hover"
    >
      <template #reference>
        <el-badge :value="localQuantity" class="cart-badge">
          <el-button
              type="primary"
              circle
              @click.stop="toggleCart"
              :disabled="stock === 0"
          >
            <el-icon><ShoppingCart /></el-icon>
          </el-button>
        </el-badge>
      </template>
      <div class="quantity-control">
        <el-button
            circle
            :disabled="localQuantity <= 0"
            @click.stop="updateQuantity(-1)"
        >
          <el-icon><Minus /></el-icon>
        </el-button>
        <span class="quantity">{{ localQuantity }}</span>
        <el-button
            circle
            :disabled="localQuantity >= stock"
            @click.stop="updateQuantity(1)"
        >
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>
    </el-popover>
    <el-button
        v-else
        type="primary"
        circle
        @click.stop="toggleCart"
        :disabled="stock === 0"
    >
      <el-icon><ShoppingCart /></el-icon>
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ShoppingCart, Plus, Minus } from '@element-plus/icons-vue'
import { getCart, addToCart, updateCartItemQuantity, deleteCartItem } from '../api/cart'

const props = defineProps<{
  productId: string
  stock: number
}>()

const localQuantity = ref(0)
const cartItemId = ref('')

const loadCartData = async () => {
  try {
    const res = await getCart()
    const item = res.data.data.items.find((i: any) => i.productId === props.productId)
    if (item) {
      localQuantity.value = item.quantity
      cartItemId.value = item.cartItemId
    }
  } catch (error) {
    ElMessage.error('获取购物车信息失败')
  }
}

const updateQuantity = async (delta: number) => {
  const newQuantity = localQuantity.value + delta
  if (newQuantity < 0 || newQuantity > props.stock) return

  try {
    if (newQuantity === 0) {
      await deleteCartItem(cartItemId.value)
    } else if (localQuantity.value === 0) {
      await addToCart(props.productId, newQuantity)
    } else {
      await updateCartItemQuantity(cartItemId.value, newQuantity)
    }
    await loadCartData()
  } catch (error) {
    ElMessage.error('操作失败，请重试')
  }
}

const toggleCart = async () => {
  if (localQuantity.value === 0) {
    await updateQuantity(1)
  }
}

onMounted(loadCartData)
</script>

<style scoped>
.cart-operations {
  display: inline-flex;
  margin-left: 8px;
}

.quantity-control {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}

.quantity {
  margin: 0 12px;
  font-weight: 500;
}

.cart-badge {
  :deep(.el-badge__content) {
    top: 12px;
    right: 12px;
  }
}
</style>
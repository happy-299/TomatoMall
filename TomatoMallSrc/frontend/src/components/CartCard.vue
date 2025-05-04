<script setup lang="ts">
import type { CartItem } from '../api/cart';

defineProps<{
  item: CartItem;
  stock: number;
  highlighted: boolean;
}>();

defineEmits(['quantity-change', 'delete', 'view-product']);
</script>

<template>
  <div class="cart-item" :class="{ 'highlighted-item': highlighted }">
    <div class="item-left">
      <el-checkbox :label="item.cartItemId" class="item-check" />
      <img
          :src="item.cover || 'https://via.placeholder.com/100'"
          class="product-cover"
          @click="$emit('view-product', item.productId)"
      />
    </div>

    <div class="item-info" @click="$emit('view-product', item.productId)">
      <h3 class="title">{{ item.title }}</h3>
      <p class="description">{{ item.description }}</p>
      <div class="price-stock">
        <span class="price">¥{{ item.price.toFixed(2) }}</span>
        <span class="stock">库存: {{ stock }}</span>
      </div>
    </div>

    <div class="item-actions">
      <div class="quantity-control">
        <el-button
            circle
            size="small"
            :disabled="item.quantity <= 1"
            @click.stop="$emit('quantity-change', { item, type: 'subtract' })"
        >
          -
        </el-button>
        <span class="quantity">{{ item.quantity }}</span>
        <el-button
            circle
            size="small"
            :disabled="item.quantity >= stock"
            @click.stop="$emit('quantity-change', { item, type: 'add' })"
        >
          +
        </el-button>
      </div>
      <el-button
          type="danger"
          size="small"
          @click.stop="$emit('delete', item.cartItemId)"
      >
        删除
      </el-button>
    </div>
  </div>
</template>

<style scoped>
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
  color: #333;
  font-size: 14px;
  padding: 0 8px;
}
</style>
<template>
  <div class="my-coupons">
    <div class="header">
      <h2>我的优惠券</h2>
      <el-button link @click="router.push('/coupons')">
        返回优惠券广场
      </el-button>
    </div>

    <!-- 优惠券列表 -->
    <div class="coupon-list">
      <el-card v-for="coupon in userCoupons" :key="coupon.id" class="coupon-card">
        <div class="coupon-content">
          <div class="coupon-info">
            <h3>{{ coupon.title }}</h3>
            <p>{{ coupon.description }}</p>
            <div class="coupon-details">
              <span>满{{ coupon.threshold }}减{{ coupon.reduce }}</span>
              <span>有效期至: {{ coupon.expiryDateTime }}</span>
              <span :class="{ 'expired': isExpired(coupon.expiryDateTime) }">
                {{ isExpired(coupon.expiryDateTime) ? '已过期' : '可使用' }}
              </span>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { router } from '../router'
import { getUserCoupons } from '../api/coupon'
import type { UserCoupon } from '../types/coupon'

const userCoupons = ref<UserCoupon[]>([])

// 获取用户优惠券列表
const fetchUserCoupons = async () => {
  try {
    const res = await getUserCoupons()
    if (res.data.code === '200') {
      userCoupons.value = res.data.data
    }
  } catch (error) {
    console.error('获取用户优惠券失败:', error)
    ElMessage.error('获取用户优惠券失败')
  }
}

// 检查优惠券是否过期
const isExpired = (expiryDateTime: string) => {
  const expiry = new Date(expiryDateTime.replace(/_/g, ' '))
  return expiry < new Date()
}

onMounted(() => {
  fetchUserCoupons()
})
</script>

<style scoped>
.my-coupons {
  padding: 20px 40px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding: 0 10px;
}

.header h2 {
  font-size: 24px;
  color: #2c698d;
  margin: 0;
}

.header :deep(.el-button--text) {
  font-size: 16px;
  color: #2c698d;
  padding: 8px 16px;
}

.header :deep(.el-button--text:hover) {
  color: #409EFF;
  background-color: rgba(64, 158, 255, 0.1);
  border-radius: 4px;
}

.coupon-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 25px;
  padding: 0 10px;
}

.coupon-card {
  transition: all 0.3s;
  border-radius: 8px;
  overflow: hidden;
}

.coupon-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.coupon-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 5px;
}

.coupon-info h3 {
  margin: 0 0 12px 0;
  color: #2c698d;
  font-size: 18px;
}

.coupon-info p {
  color: #666;
  margin: 0 0 15px 0;
  line-height: 1.5;
}

.coupon-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #666;
  font-size: 14px;
  padding: 10px 0;
  border-top: 1px dashed #eee;
}

.expired {
  color: #f56c6c;
  font-weight: 500;
}
</style> 
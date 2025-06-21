<template>
  <div class="coupon-square">
    <!-- 现代化Banner -->
    <div class="banner">
      <div class="banner-overlay">
        <div class="banner-content">
          <h1>优惠券广场</h1>
          <p>发现更多优惠，享受购物乐趣</p>
          <el-button type="primary" size="large" @click="router.push('/my-coupons')">
            查看我的优惠券
          </el-button>
        </div>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 页面标题和操作栏 -->
      <div class="page-header">
        <div class="header-left">
          <h2>热门优惠券</h2>
          <p class="subtitle">精选优质优惠券，助您省钱购物</p>
        </div>
      </div>

      <!-- 优惠券列表 -->
      <div class="coupon-list">
        <el-card v-for="template in couponTemplates" :key="template.id" class="coupon-card" :class="{ 'expired': isExpired(template.expiryDateTime) }">
          <div class="coupon-content">
            <div class="coupon-header">
              <div class="coupon-amount-section">
                <div class="coupon-amount">
                  <span class="currency">¥</span>
                  <span class="amount">{{ template.reduce }}</span>
                </div>
                <div class="coupon-condition">
                  满{{ template.threshold }}可用
                </div>
              </div>
            </div>
            <div class="coupon-body">
              <h3 class="coupon-title">{{ template.title }}</h3>
              <p class="coupon-desc">{{ template.description }}</p>
              <div class="coupon-meta">
                <span class="meta-item">
                  <el-icon><Clock /></el-icon>
                  {{ template.expiryDateTime }}
                </span>
                <span class="meta-item">
                  <el-icon><Tickets /></el-icon>
                  剩余{{ template.restCnt }}张
                </span>
              </div>
            </div>
            <div class="coupon-footer">
              <el-button 
                type="primary" 
                :disabled="!template.inUse || template.restCnt <= 0 || isExpired(template.expiryDateTime)"
                @click="handleReceive(template.id)"
                class="receive-btn"
              >
                {{ getButtonText(template) }}
              </el-button>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 空状态 -->
      <div v-if="couponTemplates.length === 0" class="empty-state">
        <el-empty description="暂无优惠券" />
      </div>
    </div>

    <!-- 创建优惠券按钮 -->
    <el-button
      v-if="isAdmin"
      type="primary"
      class="create-button"
      circle
      @click="showCreateDialog = true"
    >
      <el-icon class="plus-icon"><Plus /></el-icon>
    </el-button>

    <!-- 创建优惠券对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建优惠券"
      width="500px"
    >
      <el-form :model="newCoupon" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="newCoupon.title" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="newCoupon.description" type="textarea" />
        </el-form-item>
        <el-form-item label="门槛金额">
          <el-input-number v-model="newCoupon.threshold" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="优惠金额">
          <el-input-number v-model="newCoupon.reduce" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="发放数量">
          <el-input-number v-model="newCoupon.restCnt" :min="1" :precision="0" />
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="newCoupon.expiryDateTime"
            type="datetime"
            placeholder="选择过期时间"
            format="YYYY-MM-DD_HH-mm-ss"
            value-format="YYYY-MM-DD_HH-mm-ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showCreateDialog = false">取消</el-button>
          <el-button type="primary" @click="handleCreate">创建</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { router } from '../router'
import { Plus, Clock, Tickets, Document } from '@element-plus/icons-vue'
import { getAllCouponTemplates, receiveCoupon, checkCouponReceived, createCouponTemplate } from '../api/coupon'
import type { CouponTemplate } from '../types/coupon'

const isAdmin = sessionStorage.getItem('role') === 'admin'
const couponTemplates = ref<CouponTemplate[]>([])
const showCreateDialog = ref(false)
const receivedCoupons = ref<Set<number>>(new Set())

const newCoupon = ref({
  title: '',
  description: '',
  type: 'FULL_REDUCTION' as const,
  threshold: 0,
  reduce: 0,
  discount: 0,
  inUse: true,
  restCnt: 100,
  expiryDateTime: '',
  img: 'no'
})

// 检查优惠券是否已领取
const checkCouponReceivedStatus = async (templateId: number) => {
  try {
    const checkRes = await checkCouponReceived(templateId)
    if (checkRes.data.data) {
      receivedCoupons.value.add(templateId)
    }
  } catch (error) {
    console.error('检查优惠券领取状态失败:', error)
  }
}

// 获取优惠券模板列表
const fetchCouponTemplates = async () => {
  try {
    const res = await getAllCouponTemplates()
    if (res.data.code === '200') {
      couponTemplates.value = res.data.data
      // 检查每个优惠券的领取状态
      for (const template of couponTemplates.value) {
        await checkCouponReceivedStatus(template.id)
      }
    }
  } catch (error) {
    console.error('获取优惠券模板失败:', error)
    ElMessage.error('获取优惠券模板失败')
  }
}

// 领取优惠券
const handleReceive = async (templateId: number) => {
  try {
    // 检查是否已领取
    const checkRes = await checkCouponReceived(templateId)
    if (checkRes.data.data) {
      ElMessage.warning('您已领取过该优惠券')
      receivedCoupons.value.add(templateId)
      return
    }

    const res = await receiveCoupon(templateId)
    if (res.data.code === '200') {
      ElMessage.success('领取成功')
      receivedCoupons.value.add(templateId)
      fetchCouponTemplates() // 刷新列表
    }
  } catch (error) {
    console.error('领取优惠券失败:', error)
    ElMessage.error('领取优惠券失败')
  }
}

// 创建优惠券
const handleCreate = async () => {
  try {
    // 表单验证
    if (!newCoupon.value.title.trim()) {
      ElMessage.warning('请输入优惠券标题')
      return
    }
    if (!newCoupon.value.description.trim()) {
      ElMessage.warning('请输入优惠券描述')
      return
    }
    if (newCoupon.value.threshold <= 0) {
      ElMessage.warning('门槛金额必须大于0')
      return
    }
    if (newCoupon.value.reduce <= 0) {
      ElMessage.warning('优惠金额必须大于0')
      return
    }
    if (newCoupon.value.reduce >= newCoupon.value.threshold) {
      ElMessage.warning('优惠金额必须小于门槛金额')
      return
    }
    if (newCoupon.value.restCnt <= 0) {
      ElMessage.warning('发放数量必须大于0')
      return
    }
    if (!newCoupon.value.expiryDateTime) {
      ElMessage.warning('请选择过期时间')
      return
    }

    // 确保日期时间格式正确
    const formattedDateTime = newCoupon.value.expiryDateTime.replace('T', '_').replace(':', '-')
    const couponData = {
      ...newCoupon.value,
      expiryDateTime: formattedDateTime
    }

    const res = await createCouponTemplate(couponData)
    if (res.data.code === '200') {
      ElMessage.success('创建成功')
      showCreateDialog.value = false
      // 重置表单
      newCoupon.value = {
        title: '',
        description: '',
        type: 'FULL_REDUCTION',
        threshold: 0,
        reduce: 0,
        discount: 0,
        inUse: true,
        restCnt: 100,
        expiryDateTime: '',
        img: 'no'
      }
      fetchCouponTemplates() // 刷新列表
    }
  } catch (error) {
    console.error('创建优惠券失败:', error)
    ElMessage.error('创建优惠券失败')
  }
}

const isExpired = (expiryDateTime: string) => {
  const currentDateTime = new Date().toISOString().split('T')[0] + 'T' + new Date().toISOString().split('T')[1]
  return expiryDateTime < currentDateTime
}

const getButtonText = (template: CouponTemplate) => {
  if (isExpired(template.expiryDateTime)) {
    return '已过期'
  }
  if (receivedCoupons.value.has(template.id)) {
    return '你已经领取过'
  }
  if (template.restCnt <= 0) {
    return '已抢完'
  }
  return '立即领取'
}

onMounted(() => {
  fetchCouponTemplates()
})
</script>

<style scoped>
.coupon-square {
  position: relative;
  min-height: calc(100vh - 60px);
}

.banner {
  position: relative;
  height: 500px;
  margin-bottom: 40px;
  border-radius: 8px;
  overflow: hidden;
  background-image: url('https://images.unsplash.com/photo-1607083206968-13611e3d76db?q=80&w=2115&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.banner-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.banner-content {
  text-align: center;
}

.banner-content h1 {
  font-size: 36px;
  color: #fff;
  margin-bottom: 15px;
  font-weight: 600;
}

.banner-content p {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 25px;
}

.banner-content :deep(.el-button),
.receive-btn,
.create-button,
.el-dialog__footer .el-button--primary {
  background: linear-gradient(135deg, #ff6347 0%, #ff4d29 100%) !important;
  border: none !important;
  color: #fff !important;
}

.banner-content :deep(.el-button):hover,
.receive-btn:hover:not(:disabled),
.create-button:hover,
.el-dialog__footer .el-button--primary:hover {
  background: linear-gradient(135deg, #ff8266 0%, #ff6347 100%) !important;
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(255, 99, 71, 0.25);
}

.receive-btn:disabled {
  background: #ffd6cc !important;
  color: #ff6347 !important;
}

.create-button {
  background: linear-gradient(135deg, #ff6347 0%, #ff4d29 100%) !important;
  color: #fff !important;
  border: none !important;
}

.create-button:hover {
  background: linear-gradient(135deg, #ff8266 0%, #ff6347 100%) !important;
  color: #fff !important;
}

.main-content {
  padding: 0 40px 40px 40px;
}

.page-header {
  margin-bottom: 30px;
}

.header-left {
  flex: 1;
}

.header-left h2 {
  font-size: 24px;
  color: #2c698d;
  margin: 0;
}

.header-left p {
  font-size: 16px;
  color: #666;
  margin: 5px 0 0 0;
}

.header-right {
  flex: 0;
}

.header-right :deep(.el-button) {
  padding: 8px 16px;
  font-size: 14px;
}

.coupon-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 25px;
  padding: 0 40px;
}

.coupon-card {
  transition: all 0.3s;
  border-radius: 12px;
  overflow: hidden;
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.coupon-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.coupon-content {
  display: flex;
  flex-direction: column;
  padding: 0;
  position: relative;
}

.coupon-header {
  display: flex;
  align-items: center;
  padding: 20px 20px 15px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  position: relative;
}

.coupon-header::after {
  content: '';
  position: absolute;
  bottom: -10px;
  left: 20px;
  right: 20px;
  height: 20px;
  background: inherit;
  border-radius: 0 0 10px 10px;
  z-index: 1;
}

.coupon-amount-section {
  display: flex;
  align-items: center;
  gap: 15px;
  flex: 1;
}

.coupon-amount {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.currency {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.9);
}

.amount {
  font-size: 32px;
  font-weight: bold;
  color: white;
}

.coupon-condition {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20px;
}

.coupon-body {
  padding: 25px 20px 20px 20px;
  background: white;
  position: relative;
  z-index: 2;
}

.coupon-title {
  margin: 0 0 10px 0;
  color: #2c3e50;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
}

.coupon-desc {
  color: #7f8c8d;
  margin: 0 0 15px 0;
  line-height: 1.6;
  font-size: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.coupon-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6c757d;
  font-size: 13px;
}

.meta-item .el-icon {
  font-size: 14px;
  color: #667eea;
}

.coupon-footer {
  display: flex;
  justify-content: center;
  padding: 0 20px 20px 20px;
}

.receive-btn {
  width: 100%;
  padding: 12px 20px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  transition: all 0.3s;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.receive-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.receive-btn:disabled {
  background: #bdc3c7;
  color: #7f8c8d;
}

.create-button {
  position: fixed;
  right: 50px;
  top: 100px;
  width: 56px !important;
  height: 56px !important;
  padding: 0 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  transition: all 0.3s;
  z-index: 1000;
}

.plus-icon {
  font-size: 24px;
  margin: 0 !important;
}

.create-button:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.coupon-card.expired {
  opacity: 0.6;
  filter: grayscale(0.8);
}

.coupon-card.expired .coupon-header {
  background: linear-gradient(135deg, #9ca3af 0%, #6b7280 100%);
}

.coupon-card.expired .coupon-title {
  color: #9ca3af;
}

.coupon-card.expired .coupon-desc {
  color: #d1d5db;
}

.coupon-card.expired .coupon-meta {
  background: #f3f4f6;
}

.coupon-card.expired .meta-item {
  color: #9ca3af;
}

.coupon-card.expired .meta-item .el-icon {
  color: #9ca3af;
}

.receive-btn:disabled {
  background: #d1d5db !important;
  color: #6b7280 !important;
  cursor: not-allowed;
}
</style> 
<template>
  <div class="coupon-square">
    <div class="header">
      <h2>优惠券广场</h2>
      <el-button link @click="router.push('/my-coupons')">
        我的优惠券
      </el-button>
    </div>

    <!-- 优惠券列表 -->
    <div class="coupon-list">
      <el-card v-for="template in couponTemplates" :key="template.id" class="coupon-card">
        <div class="coupon-content">
          <div class="coupon-info">
            <h3>{{ template.title }}</h3>
            <p>{{ template.description }}</p>
            <div class="coupon-details">
              <span>满{{ template.threshold }}减{{ template.reduce }}</span>
              <span>剩余: {{ template.restCnt }}张</span>
              <span>有效期至: {{ template.expiryDateTime }}</span>
            </div>
          </div>
          <div class="coupon-actions">
            <el-button 
              type="primary" 
              :disabled="!template.inUse || template.restCnt <= 0"
              @click="handleReceive(template.id)"
            >
              立即领取
            </el-button>
          </div>
        </div>
      </el-card>
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
import { Plus } from '@element-plus/icons-vue'
import { getAllCouponTemplates, receiveCoupon, checkCouponReceived, createCouponTemplate } from '../api/coupon'
import type { CouponTemplate } from '../types/coupon'

const isAdmin = sessionStorage.getItem('role') === 'admin'
const couponTemplates = ref<CouponTemplate[]>([])
const showCreateDialog = ref(false)

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

// 获取优惠券模板列表
const fetchCouponTemplates = async () => {
  try {
    const res = await getAllCouponTemplates()
    if (res.data.code === '200') {
      couponTemplates.value = res.data.data
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
      return
    }

    const res = await receiveCoupon(templateId)
    if (res.data.code === '200') {
      ElMessage.success('领取成功')
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

onMounted(() => {
  fetchCouponTemplates()
})
</script>

<style scoped>
.coupon-square {
  padding: 20px 40px;
  position: relative;
  min-height: calc(100vh - 60px);
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

.coupon-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 10px;
}

.coupon-actions :deep(.el-button) {
  padding: 10px 24px;
  font-size: 14px;
}

.create-button {
  position: fixed;
  right: 50px;
  bottom: 50px;
  width: 56px !important;
  height: 56px !important;
  padding: 0 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  transition: all 0.3s;
}

.plus-icon {
  font-size: 24px;
  margin: 0 !important;
}

.create-button:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);
}
</style> 
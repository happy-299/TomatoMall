<template>
  <div class="payment-container">
    <el-card class="payment-card" v-loading="loading">
      <!-- 支付成功状态 -->
      <div v-if="paymentStatus === 'success'" class="payment-success">
        <el-icon color="#67C23A" :size="60"><Check /></el-icon>
        <h2>支付成功！</h2>
        <p class="info-item">订单号：{{ orderId }}</p>
        <p class="info-item">支付金额：¥{{ totalAmount.toFixed(2) }}</p>
        <!-- 修复 div 标签不闭合 -->
        <div class="action-buttons">
          <el-button type="primary" @click="router.push('/')">返回首页</el-button>
          <el-button @click="router.push(`/orders/${orderId}`)">查看订单</el-button>
        </div>
      </div>

      <!-- 支付失败状态 -->
      <div v-else-if="paymentStatus === 'failed'" class="payment-failed">
        <el-icon color="#F56C6C" :size="60"><Close /></el-icon>
        <h2>支付失败</h2>
        <p class="error-msg">{{ errorMessage || '支付过程中出现错误' }}</p>
        <div class="retry-section">
          <el-button @click="router.push('/cart')">返回购物车</el-button>
        </div>
      </div>

      <!-- 支付信息展示 -->
      <div v-else class="payment-info">
        <h2 class="payment-title">订单支付</h2>
        <div class="order-header">
          <p>订单号：{{ orderId }}</p>
          <p>支付金额：¥{{ totalAmount.toFixed(2) }}</p>
        </div>

        <div class="payment-footer">
          <div class="countdown">
            <el-icon><Clock /></el-icon>
            剩余支付时间：{{ minutes }}:{{ seconds }}
          </div>
          <el-button
              type="primary"
              class="payment-button"
              @click="handlePayment"
          >
            <el-icon><CreditCard /></el-icon>
            立即支付
          </el-button>
          <!-- 新增取消支付按钮 -->
          <el-button
              type="warning"
              class="cancel-button"
              @click="handleCancelPayment"
          >
            <el-icon><Close /></el-icon>
            取消订单
          </el-button>

        </div>
      </div>
    </el-card>
  </div>

</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter, useRoute } from 'vue-router'
import { Check, Close, Clock, CreditCard } from '@element-plus/icons-vue'
// 修复导入语句
import {payOrder, getOrderStatus, type RetPay, cancelOrder} from '../../api/order'

const router = useRouter()
const route = useRoute()

type PaymentStatus = 'pending' | 'success' | 'failed'
const paymentStatus = ref<PaymentStatus>('pending')
const loading = ref(false)
const errorMessage = ref('')
const timeLeft = ref(1800) // 30分钟倒计时

// 从路由参数获取订单信息
const orderId = ref(route.query.orderId?.toString() || '')
const totalAmount = ref(Number(route.query.amount) || 0)

// 倒计时计算
const minutes = computed(() => Math.floor(timeLeft.value / 60).toString().padStart(2, '0'))
const seconds = computed(() => (timeLeft.value % 60).toString().padStart(2, '0'))

// 新增取消支付处理逻辑
const handleCancelPayment = async () => {
  try {
    loading.value = true;

    // 严格校验订单ID
    const rawId = orderId.value;
    if (!rawId || !/^\d+$/.test(rawId)) {
      throw new Error('订单ID格式错误');
    }
    const orderIdNum = Number(rawId);

    await ElMessageBox.confirm('确定要取消该订单吗？', '取消确认', {
      confirmButtonText: '确定取消',
      cancelButtonText: '继续支付',
      type: 'warning'
    });

    await cancelOrder(orderIdNum); // 传递数字类型ID
    paymentStatus.value = 'failed'; // 使用独立状态
    ElMessage.success('订单已取消');
  } catch (error: any) {
    if (error !== 'cancel') {
      const errMsg = error.response?.data?.msg || error.message;
      ElMessage.error(`取消失败: ${errMsg}`);
    }
  } finally {
    loading.value = false;
  }
}

// 支付处理
const handlePayment = async () => {
  try {
    loading.value = true
    const res: RetPay = await payOrder(orderId.value)

    // 渲染支付宝表单
    const container = document.createElement('div')
    container.innerHTML = res.paymentForm
    document.body.appendChild(container)

    const form = container.querySelector('form')
    if (form) {
      form.submit()
    } else {
      throw new Error('支付表单解析失败')
    }

    startPolling()
  } catch (error: any) {
    paymentStatus.value = 'failed'
    errorMessage.value = error.message || '支付请求失败'
    console.error('失败:', error)
  } finally {
    loading.value = false
  }
}

// 支付状态轮询
let pollingInterval: number
const startPolling = () => {
  pollingInterval = window.setInterval(async () => {
    try {
      const { status } = await getOrderStatus(orderId.value)
      if (status === 'PAID') {
        paymentStatus.value = 'success'
        clearIntervals()
      }
    } catch (error) {
      console.error('状态查询失败:', error)
    }
  }, 5000)
}

// 倒计时处理
let countdownTimer: number
const startCountdown = () => {
  countdownTimer = window.setInterval(() => {
    if (timeLeft.value > 0) {
      timeLeft.value--
    } else {
      paymentStatus.value = 'failed'
      errorMessage.value = '支付超时'

      clearIntervals()
    }
  }, 1000)
}

// 重新支付
// 修复赋值操作符


// 清理定时器
const clearIntervals = () => {
  clearInterval(countdownTimer)
  clearInterval(pollingInterval)
}

// 参数校验
onMounted(() => {
  if (!orderId.value || totalAmount.value <= 0) {
    ElMessage.error('无效的订单参数')
    router.push('/cart')
  } else {
    startCountdown()
  }
})

onBeforeUnmount(clearIntervals)
</script>

<style scoped>
/* 修复样式选择器分隔符 */
.payment-container {
  padding: 40px 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  min-height: 100vh;
}

.payment-card {
  max-width: 800px;
  margin: 0 auto;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.order-header {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 24px;
}

.payment-footer {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #eee;
}

.countdown {
  color: #e67e22;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.payment-button {
  width: 100%;
  margin-top: 24px;
  padding: 16px;
  margin-bottom: 16px;
  font-size: 16px;
}

.action-buttons,
.retry-section {
  margin-top: 32px;
  display: flex;
  gap: 20px;
  justify-content: center;
}

.info-item {
  margin: 12px 0;
  color: #666;
}

.error-msg {
  color: #f56c6c;
  margin: 20px 0;
}
</style>
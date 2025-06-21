<template>
  <el-skeleton v-if="loading" :rows="5" animated/>
  <div v-else class="verification-review">
    <div class="review-header">
      <h2 class="review-title">认证申请审核</h2>
      <p class="review-subtitle">管理用户认证申请，维护文学社区品质</p>
    </div>
    
    <div class="tabs-container">
      <el-tabs v-model="activeStatus" class="custom-tabs">
        <el-tab-pane label="待审核" name="PENDING">
          <template #label>
            <div class="tab-label">
              <el-icon><Clock /></el-icon>
              <span>待审核</span>
              <el-badge v-if="pendingCount > 0" :value="pendingCount" class="pending-badge" />
            </div>
          </template>
        </el-tab-pane>
        <el-tab-pane label="已通过" name="APPROVED">
          <template #label>
            <div class="tab-label">
              <el-icon><Check /></el-icon>
              <span>已通过</span>
            </div>
          </template>
        </el-tab-pane>
        <el-tab-pane label="已拒绝" name="REJECTED">
          <template #label>
            <div class="tab-label">
              <el-icon><Close /></el-icon>
              <span>已拒绝</span>
            </div>
          </template>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 申请列表 -->
    <div class="application-list">
      <template v-if="applications.length">
        <div v-for="app in applications" :key="app.id" class="application-card">
          <!-- 用户卡片区域 (左侧) -->
          <div class="user-card-container">
            <user-card :user="{
              id: app.id,
              username: app.username,
              avatar: app.avatar,
              isVerified: app.verifiedName !== null,
              bio: '',
              verifiedName: app.verifiedName || ''
            }"/>
          </div>
          
          <!-- 信息和操作区域 (右侧) -->
          <div class="info-action-container">
            <!-- 申请信息 -->
            <div class="compact-info">
              <div class="info-item">
                <span class="label">认证类型：</span>
                <span class="value">{{ app.verifiedName || '普通认证' }}</span>
              </div>
              <div class="info-item">
                <span class="label">申请状态：</span>
                <el-tag :type="app.status === 'APPROVED' ? 'success' : app.status === 'REJECTED' ? 'danger' : 'warning'" size="small">
                  {{ statusMap[app.status as keyof typeof statusMap] }}
                </el-tag>
              </div>
              <div v-if="app.reviewTime" class="info-item">
                <span class="label">审核时间：</span>
                <span class="value">{{ formatTime(app.reviewTime) }}</span>
              </div>
            </div>
            
            <div v-if="app.rejectReason" class="reject-reason">
              <span class="reason-label">拒绝原因：</span>
              <span class="reason-text">{{ app.rejectReason }}</span>
            </div>

            <!-- 操作区域 -->
            <div class="card-footer">
              <el-button class="detail-btn" size="small" @click="openDetail(app)">
                <el-icon><Document /></el-icon>
                申请资料
              </el-button>
              
              <div v-if="activeStatus === 'PENDING'" class="action-buttons">
                <el-button type="success" size="small" @click="handleReview(app.id, true)">通过</el-button>
                <el-button type="danger" size="small" @click="openRejectDialog(app)">拒绝</el-button>
              </div>
            </div>
          </div>
        </div>
      </template>
      <div v-else class="empty-state">
        暂无相关申请
      </div>
    </div>

    <!-- 申请资料弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="申请详细信息" width="600px">
      <div class="detail-content" v-if="currentDetailApp">
        <div class="detail-item">
          <h4>认证名号</h4>
          <p>{{ currentDetailApp.verifiedName || '未填写认证名号' }}</p>
        </div>
        <div class="detail-item">
          <h4>申请理由</h4>
          <p>{{ currentDetailApp.reasonText || '未填写申请理由' }}</p>
        </div>
        <div class="detail-item">
          <h4>证明材料</h4>
          <div class="material-grid">
            <el-image
                v-for="(img, index) in currentDetailApp.proofImgs"
                :key="index"
                :src="img"
                fit="cover"
                class="material-thumbnail"
                :preview-src-list="currentDetailApp.proofImgs"
            >
              <template #error>
                <div class="image-error">
                  <el-icon>
                    <Picture/>
                  </el-icon>
                </div>
              </template>
            </el-image>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadApplications"
          @size-change="handleSizeChange"
      />
    </div>

    <!-- 拒绝原因对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="输入拒绝原因" width="30%">
      <el-input
          v-model="rejectReason"
          type="textarea"
          :rows="3"
          placeholder="请输入拒绝理由..."
      />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>


<script setup lang="ts">
import {ref, watch, onMounted, computed} from 'vue'
import {Picture, Clock, Check, Close, Document} from '@element-plus/icons-vue'
import UserCard from '../../components/VUserCard.vue'
import {
  reviewVerification,
  getVerificationListByStatus
} from '../../api/verification'
import {getUserInfo} from '../../api/user'
import {ElMessage} from 'element-plus'
import dayjs from 'dayjs'

const loading = ref(false)
const activeStatus = ref<'PENDING' | 'APPROVED' | 'REJECTED'>('PENDING')
const applications = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const detailDialogVisible = ref(false)
const currentDetailApp = ref<any>(null)
// 拒绝相关状态
const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const currentRejectApp = ref<any>(null)

// 计算待审核数量
const pendingCount = ref(0)

const formatTime = (timestamp: number) => {
  return dayjs(timestamp).format('YYYY-MM-DD HH:mm')
}

const openDetail = (app: any) => {
  currentDetailApp.value = app
  detailDialogVisible.value = true
}

const statusMap = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝'
}

// 加载申请列表
const loadApplications = async () => {
  try {
    loading.value = true
    const res = await getVerificationListByStatus(
        activeStatus.value,
        currentPage.value - 1, // 修复页码转换
        pageSize.value
    )
    console.log("res => ",res)
    total.value = res.data.data.totalElements || res.data.data.total || 0 // 兼容不同分页字段

    applications.value = res.data.data.content
    console.log("app => ",applications)
    
    // 如果当前不是待审核标签，获取待审核数量
    if (activeStatus.value !== 'PENDING') {
      try {
        const pendingRes = await getVerificationListByStatus('PENDING', 0, 1)
        pendingCount.value = pendingRes.data.data.totalElements || pendingRes.data.data.total || 0
      } catch (error) {
        console.error('获取待审核数量失败:', error)
      }
    } else {
      pendingCount.value = total.value
    }
  } catch (error) {
    console.error('加载申请失败:', error)
    ElMessage.error('数据加载失败')
  } finally {
    loading.value = false
  }
}

// 处理分页大小变化
const handleSizeChange = (newSize: number) => {
  pageSize.value = newSize
  currentPage.value = 1
  loadApplications()
}

// 打开拒绝对话框
const openRejectDialog = (app: any) => {
  currentRejectApp.value = app
  rejectDialogVisible.value = true
}

// 确认拒绝
const confirmReject = async () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }

  await reviewVerification({
    appId: currentRejectApp.value.id,
    pass: false,
    rejectReason: rejectReason.value
  })

  rejectDialogVisible.value = false
  rejectReason.value = ''
  loadApplications()
}

// 处理审核通过
const handleReview = async (appId: number, pass: boolean) => {
  await reviewVerification({
    appId,
    pass,
    rejectReason: pass ? undefined : '' // 实际不会用到
  })
  loadApplications()
}

// 监听状态变化重置分页
watch(activeStatus, () => {
  currentPage.value = 1
  loadApplications()
})

onMounted(loadApplications)
</script>

<style scoped>
.verification-review {
  padding: 80px 10px 20px 10px;
  max-width: 1500px;
  margin: 0 auto;
}

.review-header {
  text-align: center;
  margin-bottom: 40px;
}

.review-title {
  font-size: 32px;
  color: #2c698d;
  margin: 0 0 16px 0;
  font-weight: 600;
}

.review-subtitle {
  font-size: 16px;
  color: #666;
  margin: 0;
}

.tabs-container {
  margin-bottom: 30px;
}

.custom-tabs {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.custom-tabs :deep(.el-tabs__header) {
  margin-bottom: 30px;
}

.custom-tabs :deep(.el-tabs__nav-wrap) {
  justify-content: center;
}

.custom-tabs :deep(.el-tabs__nav) {
  border-radius: 30px;
  background-color: #f8f9fa;
  padding: 5px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.custom-tabs :deep(.el-tabs__item) {
  font-size: 16px;
  padding: 0 24px;
  height: 50px;
  line-height: 50px;
  transition: all 0.3s;
  border-radius: 25px;
}

.custom-tabs :deep(.el-tabs__item.is-active) {
  color: #fff;
  background: linear-gradient(135deg, #ff6347 0%, #ff4d29 100%);
  box-shadow: 0 4px 12px rgba(255, 99, 71, 0.25);
}

.custom-tabs :deep(.el-tabs__active-bar) {
  display: none;
}

.custom-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.tab-label {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.pending-badge {
  margin-left: 4px;
}

.pending-badge :deep(.el-badge__content) {
  background-color: #ff6347;
}

.empty-state {
  text-align: center;
  color: #909399;
  padding: 40px 0;
  width: 100%;
  grid-column: 1 / -1;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.application-list {
  min-height: 300px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.application-card {
  border: none;
  border-radius: 12px;
  padding: 0;
  transition: all 0.3s;
  min-height: 200px;
  background: white;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  display: flex;
  overflow: hidden;
}

.application-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.user-card-container {
  width: 160px;
  flex-shrink: 0;
  padding: 15px;
  border-right: 1px solid #f0f2f5;
  background-color: #fcfcfc;
}

.user-card-container :deep(.user-card) {
  box-shadow: none !important;
  transform: none !important;
}

.info-action-container {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.compact-info {
  background-color: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 15px;
}

.info-item {
  display: flex;
  align-items: center;
  margin: 8px 0;
  font-size: 14px;
}

.label {
  color: #909399;
  min-width: 80px;
  font-weight: 500;
}

.value {
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
}

.reject-reason {
  background-color: rgba(245, 108, 108, 0.1);
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 15px;
  font-size: 14px;
  color: #f56c6c;
  display: flex;
}

.reason-label {
  color: #f56c6c;
  font-weight: 500;
  margin-right: 8px;
}

.reason-text {
  flex: 1;
  word-break: break-all;
}

.card-footer {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-btn {
  height: 32px;
  padding: 0 15px;
  border-radius: 16px;
  background: linear-gradient(135deg, #ff6347 0%, #ff8c69 100%);
  border: none;
  color: white;
  display: flex;
  align-items: center;
  gap: 5px;
}

.detail-btn:hover {
  background: linear-gradient(135deg, #ff8266 0%, #ff6347 100%);
  box-shadow: 0 4px 12px rgba(255, 99, 71, 0.25);
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.action-buttons .el-button {
  border-radius: 20px;
  padding: 8px 20px;
}

.action-buttons .el-button--success {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  border: none;
}

.action-buttons .el-button--danger {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
  border: none;
}

.material-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 8px;
}

.material-thumbnail {
  width: 100%;
  height: 80px;
  border-radius: 4px;
  cursor: pointer;
  background: #f5f7fa;
}

.detail-content h4 {
  margin: 12px 0 8px;
  font-size: 14px;
  color: #303133;
}

.detail-content p {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #c0c4cc;
}

.pagination-wrapper {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

/* Responsive design */
@media (max-width: 1200px) {
  .application-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .verification-review {
    padding: 60px 15px 15px 15px;
  }
  
  .review-title {
    font-size: 24px;
  }
  
  .review-subtitle {
    font-size: 14px;
  }
  
  .application-card {
    flex-direction: column;
  }
  
  .user-card-container {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #f0f2f5;
  }
  
  .custom-tabs :deep(.el-tabs__item) {
    padding: 0 16px;
    font-size: 14px;
    height: 40px;
    line-height: 40px;
  }
  
  .card-footer {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
  
  .action-buttons {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
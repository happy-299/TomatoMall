<template>
  <el-skeleton v-if="loading" :rows="5" animated/>
  <div v-else class="verification-review">
    <el-tabs v-model="activeStatus">
      <el-tab-pane label="待审核" name="PENDING"/>
      <el-tab-pane label="已通过" name="APPROVED"/>
      <el-tab-pane label="已拒绝" name="REJECTED"/>
    </el-tabs>

    <!-- 申请列表 -->
    <div class="application-list">
      <template v-if="applications.length">
        <div v-for="app in applications" :key="app.id" class="application-card">
          <div class="user-header">
            <user-card :user="{
          id: app.id,
          username: app.username,
          avatar: app.avatar,
          isVerified: app.verifiedName !== null,
          bio: '',
          verifiedName: app.verifiedName || ''
        }"/>
            <el-button class="detail-btn" size="small" @click="openDetail(app)">申请资料</el-button>
          </div>

          <div class="compact-info">
            <div class="info-item">
              <span class="label">认证类型：</span>
              <span class="value">{{ app.verifiedName || '普通认证' }}</span>
            </div>
            <div class="info-item">
              <span class="label">申请状态：</span>
              <el-tag :type="app.status === 'APPROVED' ? 'success' : 'danger'" size="small">
                {{ statusMap[app.status] }}
              </el-tag>
            </div>
          </div>

          <!-- 操作区域 -->
          <div v-if="activeStatus === 'PENDING'" class="action-buttons">
            <el-button type="success" size="small" @click="handleReview(app.id, true)">通过</el-button>
            <el-button type="danger" size="small" @click="openRejectDialog(app)">拒绝</el-button>
          </div>

          <!-- 状态显示 -->
          <div v-else class="review-status">
            <div class="status-container">
              <el-tag :type="app.status === 'APPROVED' ? 'success' : 'danger'" size="small">
                {{ statusMap[app.status] }}
              </el-tag>
              <div v-if="app.reviewTime" class="review-time">
                {{ formatTime(app.reviewTime) }}
              </div>
            </div>
          </div>
          <p v-if="app.rejectReason" class="reject-reason">拒绝原因：{{ app.rejectReason }}</p>
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
import {ref, watch, onMounted} from 'vue'
import {Picture} from '@element-plus/icons-vue'
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
.empty-state {
  text-align: center;
  color: #909399;
  padding: 40px 0;
  width: 100%;
}

.application-list {
  min-height: 300px; /* 保证加载过渡 */
}

.verification-review {
  padding: 20px;
}

.application-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.application-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px;
  transition: all 0.3s;
  min-height: 200px;
}

.application-content {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.reason-section h4,
.materials-section h4 {
  margin: 10px 0;
  color: #606266;
}

.action-buttons {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.review-status {
  margin-top: 16px;
  text-align: right;
}

.reject-reason {
  margin-top: 4px;
  font-size: 12px;
  color: #f56c6c;
  word-break: break-all;
}

.image-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.action-buttons {
  margin-top: 15px;
  text-align: right;
}

.review-status {
  margin-top: 15px;
  text-align: right;
}

.reject-reason {
  margin-top: 10px;
  color: #f56c6c;
  font-size: 13px;
}

.pagination-wrapper {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

.user-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.detail-btn {
  height: 32px;
  padding: 0 12px;
}

.compact-info {
  margin: 12px 0;
}

.info-item {
  display: flex;
  align-items: center;
  margin: 8px 0;
  font-size: 13px;
}

.label {
  color: #909399;
  min-width: 70px;
}

.value {
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
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

.review-status {
  margin-top: 15px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.status-container {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.review-time {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
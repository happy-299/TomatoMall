<template>
  <el-skeleton v-if="loading" :rows="5" animated />
  <div v-else class="verification-review">
    <el-tabs v-model="activeStatus">
      <el-tab-pane label="待审核" name="PENDING" />
      <el-tab-pane label="已通过" name="APPROVED" />
      <el-tab-pane label="已拒绝" name="REJECTED" />
    </el-tabs>

    <!-- 申请列表 -->
    <div class="application-list">
      <div v-for="app in applications" :key="app.id" class="application-card">
        <user-card :user="app.user" />

        <div class="application-content">
          <div class="reason-section">
            <h4>认证理由</h4>
            <p>{{ app.reasonText || '未填写申请理由' }}</p>
          </div>

          <div class="materials-section">
            <h4>申请材料</h4>
            <div class="material-images">
              <el-image
                  v-for="(img, index) in app.materialUrls"
                  :key="index"
                  :src="img"
                  fit="cover"
                  class="material-image"
                  :preview-src-list="app.materialUrls"
              >
                <template #error>
                  <div class="image-error">
                    <el-icon :size="20"><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
            </div>
          </div>
        </div>

        <!-- 操作区域 -->
        <div v-if="activeStatus === 'PENDING'" class="action-buttons">
          <el-button type="success" @click="handleReview(app.id, true)">通过</el-button>
          <el-button type="danger" @click="openRejectDialog(app)">拒绝</el-button>
        </div>

        <div v-else class="review-status">
          <el-tag :type="app.status === 'APPROVED' ? 'success' : 'danger'">
            {{ statusMap[app.status] }}
          </el-tag>
          <p v-if="app.rejectReason" class="reject-reason">拒绝原因：{{ app.rejectReason }}</p>
        </div>
      </div>
    </div>

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
import { ref, watch, onMounted } from 'vue'
import { Picture } from '@element-plus/icons-vue'
import UserCard from '../../components/VUserCard.vue'
import {
  reviewVerification,
  getVerificationListByStatus
} from '../../api/verification'
import { getUserInfo } from '../../api/user'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const activeStatus = ref<'PENDING' | 'APPROVED' | 'REJECTED'>('PENDING')
const applications = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 拒绝相关状态
const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const currentRejectApp = ref<any>(null)

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
        currentPage.value - 1, // 后端页码从0开始
        pageSize.value
    )

    const data = res.data.data
    total.value = data.total

    // 补充用户信息
    applications.value = await Promise.all(
        data.content.map(async (app: any) => {
          const userRes = await getUserInfo(app.userId)
          return {
            ...app,
            user: userRes.data.data || { avatar: '', username: '未知用户' },
            materialUrls: app.materials?.split(',') || []
          }
        })
    )
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
  padding: 20px;
}

.application-list {
  display: grid;
  gap: 20px;
  margin-top: 20px;
}

.application-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,.1);
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

.material-images {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.material-image {
  width: 100px;
  height: 100px;
  border-radius: 4px;
  border: 1px solid #eee;
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
</style>
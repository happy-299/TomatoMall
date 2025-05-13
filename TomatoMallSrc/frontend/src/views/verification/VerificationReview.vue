<el-skeleton v-if="loading" :rows="5" animated />
<template v-else>
  <div class="verification-review">
    <el-tabs v-model="activeStatus">
      <el-tab-pane label="待审核" name="PENDING"></el-tab-pane>
      <el-tab-pane label="已审核" name="PROCESSED"></el-tab-pane>
    </el-tabs>

    <!-- 申请列表 -->
    <div class="application-list">
      <div
          v-for="app in applications"
          :key="app.id"
          class="application-item"
      >
        <div class="user-info" @click="viewUser(app.userId)">
          <img
              :src="app.user?.avatar || '/default-avatar.png'"
              class="avatar"
          />
          <span>{{ app.user?.username || '未知用户' }}</span>
        </div>

        <!-- 审核操作 -->
        <div v-if="activeStatus === 'PENDING'" class="actions">
          <el-button @click="handleReview(app.id, true)">通过</el-button>
          <el-button @click="handleReview(app.id, false)">拒绝</el-button>
        </div>

        <div v-else class="status">
          <el-tag :type="app.status === 'APPROVED' ? 'success' : 'danger'">
            {{ app.status === 'APPROVED' ? '已通过' : '已拒绝' }}
          </el-tag>
          <span v-if="app.rejectReason" class="reason">原因：{{ app.rejectReason }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { reviewVerification, getVerificationListByStatus } from '../../api/verification'
import {router} from "../../router";
import {getUserInfo} from "../../api/user.ts";
const loading = ref(false)

const activeStatus = ref<'PENDING' | 'PROCESSED'>('PENDING')
const applications = ref<any[]>([])

// 加载申请列表
const loadApplications = async () => {
  try {
    loading.value = true
    let apps = []
    if (activeStatus.value === 'PENDING') {
      const res = await getVerificationListByStatus('PENDING', 0, 100)
      apps = res.data.data.content
    } else {
      const [approvedRes, rejectedRes] = await Promise.all([
        getVerificationListByStatus('APPROVED', 0, 100),
        getVerificationListByStatus('REJECTED', 0, 100)
      ])
      apps = [...approvedRes.data.data.content, ...rejectedRes.data.data.content]
    }

    // 补充请求用户信息
    applications.value = await Promise.all(
        apps.map(async (app: any) => {
          const userRes = await getUserInfo(app.userId)
          return {
            ...app,
            user: userRes.data.data || { avatar: '', username: '未知用户' }
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

// 处理审核
const handleReview = async (appId: number, pass: boolean) => {
  await reviewVerification({
    appId,
    pass,
    rejectReason: pass ? undefined : '请补充拒绝原因' // 实际应弹出输入框
  })
  loadApplications()
}

// 查看用户详情
const viewUser = (userId: number) => {
  router.push(`/vuser-detail/${userId}`)
}

watch(activeStatus, loadApplications)
onMounted(loadApplications)
</script>
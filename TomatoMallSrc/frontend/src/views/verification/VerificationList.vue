<template>
  <div class="verification-list">
    <!-- 管理员按钮 -->
    <div v-if="isAdmin" class="admin-actions">
      <el-button
          type="primary"
          @click="router.push('/verification-review')"
      >
        审核认证用户
      </el-button>
    </div>

    <!-- 大师列表 -->
    <div class="user-grid">
      <VUserCard
          v-for="user in verifiedUsers"
          :key="user.id"
          :user="user"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getVerificationListByStatus } from '../../api/verification'
import { getUserInfo } from '../../api/user'
import VUserCard from '../../components/VUserCard.vue'

const router = useRouter()
const verifiedUsers = ref<any[]>([])
const isAdmin = computed(() => sessionStorage.getItem('role') === 'admin')

// 获取已认证用户
const loadVerifiedUsers = async () => {
  try {
    const res = await getVerificationListByStatus('APPROVED', 0, 100)
    console.log('认证申请数据:', res.data) // [!code ++]

    // 确认数据结构是否正确
    const applications = res.data.data?.content || []
    console.log('解析后的申请列表:', applications) // [!code ++]

    const users = await Promise.all(
        applications.map(async (app: any) => {
          try {
            const userRes = await getUserInfo(app.userId)
            console.log('用户详情响应:', userRes.data) // [!code ++]

            // 合并数据时显式设置 isVerified
            return {
              ...userRes.data.data,
              isVerified: true, // 强制标记认证状态 // [!code ++]
              verificationInfo: app
            }
          } catch (error) {
            console.error('获取用户失败:', app.userId, error)
            return null
          }
        })
    )

    verifiedUsers.value = users.filter(Boolean)
    console.log('最终用户列表:', verifiedUsers.value) // [!code ++]
  } catch (error) {
    console.error('加载认证用户失败:', error)
    ElMessage.error('加载认证用户失败')
  }
}

onMounted(() => {
  loadVerifiedUsers()
})
</script>

<style scoped>
.user-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
  padding: 20px;
}

.admin-actions {
  padding: 20px;
  border-bottom: 1px solid #eee;
}
</style>
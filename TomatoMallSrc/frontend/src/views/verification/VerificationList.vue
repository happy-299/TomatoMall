<template>
  <div class="verification-list">
    <!-- 管理员按钮 -->
    <div v-if="isAdmin" class="admin-actions">
      <el-button type="primary" @click="router.push('/verification-review')">
        审核认证用户
      </el-button>
    </div>

    <!-- 导航栏 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane
          v-for="category in categories"
          :key="category"
          :label="category"
          :name="category"
      >
        <!-- 用户列表 -->
        <div class="user-grid">
          <template v-if="loading">
            <el-skeleton v-for="i in 4" :key="i" animated />
          </template>
          <template v-else>
            <VUserCard
                v-for="user in categoryUsers"
                :key="user.id"
                :user="user"
            />
            <el-empty v-if="!categoryUsers.length" description="暂无认证用户" />
          </template>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getUsersByVerifiedName } from '../../api/user.ts'
import VUserCard from '../../components/VUserCard.vue'

const router = useRouter()
const categories = ['墨香雅士', '当代鲁迅', '读书达人', '藏书阁主', '笔记大师']
const activeTab = ref(categories[0])
const categoryUsers = ref<any[]>([])
const loading = ref(false)
const isAdmin = computed(() => sessionStorage.getItem('role') === 'admin')

const loadUsers = async (verifiedName: string) => {
  try {
    loading.value = true
    const res = await getUsersByVerifiedName(verifiedName)
    categoryUsers.value = res.data.data || []
  } catch (error) {
    console.error('加载用户失败:', error)
    ElMessage.error('加载用户失败')
  } finally {
    loading.value = false
  }
}

const handleTabChange = (tabName: string) => {
  loadUsers(tabName)
}

onMounted(() => {
  loadUsers(activeTab.value)
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

.el-tabs {
  margin: 0 20px;
}
</style>
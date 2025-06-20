<script setup lang="ts">
import { ref, onMounted } from 'vue'
import VUserCard from '../../components/VUserCard.vue'
import { getFollowingList } from '../../api/user.ts'
import { ElLoading } from 'element-plus'

const followingList = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getFollowingList(currentPage.value - 1, pageSize.value)
    console.log(res)
    followingList.value = res.data.data
    total.value = res.data.data.total
  } catch (error) {
    console.error('获取关注列表失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="follow-container">
      <h1 class="title">我的关注</h1>
      <div class="user-list">
        <VUserCard
            v-for="user in followingList"
            :key="user.id"
            :user="{
          id: user.id,
          username: user.username,
          avatar: user.avatar,
          isVerified: user.isVerified,
          verifiedName: user.verifiedName
        }"
        />
      </div>

      <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchData"
          class="pagination"
      />
    </div>
</template>

<style scoped>
.follow-container {
  max-width: 1200px;
  margin: 2rem auto;
  padding: 20px 40px;
}

.title {
  color: #409EFF;
  text-align: center;
  margin-bottom: 2rem;
  font-size: 2rem;
  font-weight: 600;
  letter-spacing: 1px;
}

.user-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  gap: 2rem;
  padding: 1rem;
}

@media (min-width: 1200px) {
  .user-list {
    grid-template-columns: repeat(3, 1fr);
  }
}

.pagination {
  margin: 3rem 0;
  justify-content: center;
}

/* 卡片悬停动画 */
.user-list ::v-deep(.user-card) {
  transition: all 0.3s ease;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.user-list ::v-deep(.user-card:hover) {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.15);
}
</style>
<template>
  <div class="user-card" @click="viewDetail">
    <!-- 头像部分添加默认图和加载状态 -->
    <div class="avatar-container">
      <el-image
          :src="user.avatar"
          class="avatar"
          fit="cover"
      >
        <template #error>
          <div class="avatar-error">
            <el-icon :size="40"><User /></el-icon>
          </div>
        </template>
        <template #placeholder>
          <div class="avatar-loading">
            <el-icon class="loading-icon" :size="30"><Loading /></el-icon>
          </div>
        </template>
      </el-image>
    </div>

    <!-- 用户信息区 -->
    <div class="info">
      <div class="name-wrapper">
        <h3 class="username">{{ user.username || '未命名用户' }}</h3>
        <UserBadge :is-verified="user.isVerified" :verified-name="user.verifiedName"/>
      </div>

      <!-- 附加信息 -->
      <div v-if="user.bio" class="bio">{{ user.bio }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineProps } from 'vue'
import { useRouter } from 'vue-router'
import UserBadge from './UserBadge.vue'
import { User, Loading } from '@element-plus/icons-vue'

const props = defineProps({
  user: {
    type: Object,
    required: true,
    default: () => ({
      id: 0,
      username: '',
      avatar: '',
      isVerified: false,
      bio: '',
      verifiedName: ''
    })
  }
})

const router = useRouter()
const viewDetail = () => {
  router.push(`/vuser-detail/${props.user.id}`)
}
</script>

<style scoped>
.user-card {
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
  overflow: hidden;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  }
}

.avatar-container {
  width: 100%;
  height: 200px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fa;
  margin-bottom: 12px;
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
  color: #909399;
}

.avatar-loading {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-icon {
  animation: rotate 1.5s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0); }
  to { transform: rotate(360deg); }
}

.info {
  .name-wrapper {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
  }

  .username {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    max-width: 160px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .bio {
    font-size: 12px;
    color: #606266;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}
</style>
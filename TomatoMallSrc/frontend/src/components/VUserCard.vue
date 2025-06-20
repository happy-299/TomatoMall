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
        <!-- 新增关注按钮 -->
        <el-button
            v-if="currentId != user.id"
            :type="isFollowing ? 'info' : 'primary'"
            size="small"
            @click.stop="handleFollow"
            class="follow-btn"
        >
          {{ isFollowing ? '已关注' : '关注' }}
        </el-button>
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
import { ref, watch, onMounted } from 'vue'
import { checkIsFollowed, followUser, unfollowUser } from '../api/user.ts'

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

const isFollowing = ref(false)
const currentId = Number(sessionStorage.getItem('userId'))
const router = useRouter()
// 获取关注状态
const getFollowStatus = async () => {
  try {
    const res = await checkIsFollowed(props.user.id)
    console.log("isFollowed => ",res)
    console.log("currentId => ",currentId)
    isFollowing.value = res.data.data
  } catch (error) {
    console.error('获取关注状态失败:', error)
  }
}

// 处理关注/取消关注
const handleFollow = async (e: Event) => {
  e.stopPropagation()
  try {
    if (isFollowing.value) {
      await unfollowUser(props.user.id)
    } else {
      await followUser(props.user.id)
    }
    isFollowing.value = !isFollowing.value
  } catch (error) {
    console.error('操作失败:', error)
  }
}
const viewDetail = () => {
  router.push({
    path: `/vuser-detail/${props.user.id}`,
    query: {
      avatar: props.user.avatar,
      username: props.user.username,
      isVerified: props.user.isVerified,
      verifiedName: props.user.verifiedName
    }
  })
}

onMounted(() => {
  getFollowStatus()
  // 判断是否是当前用户（需要自行实现获取当前用户ID的逻辑）
})

// 监听用户ID变化
watch(() => props.user.id, getFollowStatus)
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
  width: 100px;
  height: 100px;
  border-radius: 50%;
  margin: 0 auto 12px;
  overflow: hidden;
  position: relative;
}

/* 深度穿透处理Element组件结构 */
.avatar-container :deep(.el-image) {
  width: 100% !important;
  height: 100% !important;
  display: block !important;
}

.avatar-container :deep(.el-image__inner) {
  border-radius: inherit !important;
  width: 100% !important;
  height: 100% !important;
  object-fit: cover;
  transform: translateZ(0); /* 修复边界锯齿 */
}

.avatar-container :deep(.el-image__error),
.avatar-container :deep(.el-image__placeholder) {
  border-radius: inherit !important;
}

.follow-btn {
  margin-left: auto;
  padding: 5px 12px;
  border-radius: 16px;
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
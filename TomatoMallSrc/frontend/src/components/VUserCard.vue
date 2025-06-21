<template>
  <div class="user-card" @click="viewDetail">
    <div class="card-background"></div>
    
    <!-- 头像部分 -->
    <div class="avatar-wrapper">
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
      
      <!-- 认证标识 -->
      <div class="badge-container">
        <UserBadge :is-verified="user.isVerified" :verified-name="user.verifiedName"/>
      </div>
    </div>

    <!-- 用户信息区 -->
    <div class="info">
      <div class="name-wrapper">
        <h3 class="username">{{ user.username || '未命名用户' }}</h3>
      </div>

      <!-- 附加信息 -->
      <div v-if="user.bio" class="bio">{{ user.bio }}</div>
      
      <!-- 关注按钮 -->
      <div class="action-area">
        <el-button
            v-if="currentId != user.id"
            :type="isFollowing ? 'info' : 'primary'"
            size="small"
            @click.stop="handleFollow"
            class="follow-btn"
        >
          <el-icon v-if="isFollowing"><Check /></el-icon>
          <el-icon v-else><Plus /></el-icon>
          {{ isFollowing ? '已关注' : '关注' }}
        </el-button>
      </div>
    </div>
    
    <!-- 查看详情提示 -->
    <div class="view-detail">
      <span>查看详情</span>
      <el-icon><ArrowRight /></el-icon>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineProps } from 'vue'
import { useRouter } from 'vue-router'
import UserBadge from './UserBadge.vue'
import { User, Loading, Check, Plus, ArrowRight } from '@element-plus/icons-vue'
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
})

// 监听用户ID变化
watch(() => props.user.id, getFollowStatus)
</script>

<style scoped>
.user-card {
  position: relative;
  border-radius: 16px;
  padding: 0;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 280px; /* Reduced from 320px since we removed the stats */
}

.user-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 28px rgba(0,0,0,0.12);
}

.card-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 80px;
  background: linear-gradient(135deg, #ff6347 0%, #ff8c69 100%);
  z-index: 1;
}

.avatar-wrapper {
  position: relative;
  margin-top: 30px;
  margin-bottom: 10px;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-container {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  position: relative;
  border: 4px solid white;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  background: white;
}

.badge-container {
  margin-top: 8px;
  display: flex;
  justify-content: center;
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
  padding: 0 20px 20px;
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  z-index: 2;
}

.name-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.username {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  text-align: center;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bio {
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
  text-align: center;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 16px;
  min-height: 42px;
}

.action-area {
  display: flex;
  justify-content: center;
  margin-top: auto;
  margin-bottom: 16px;
}

.follow-btn {
  padding: 8px 20px;
  border-radius: 20px;
  font-weight: 500;
  transition: all 0.3s;
}

.follow-btn :deep(.el-icon) {
  margin-right: 4px;
}

.follow-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.view-detail {
  background: #f5f7fa;
  padding: 10px;
  text-align: center;
  color: #909399;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  transition: all 0.3s;
}

.user-card:hover .view-detail {
  background: #ff6347;
  color: white;
}
</style>
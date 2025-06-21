<template>
  <div class="verification-list">
    <!-- 现代化Banner -->
    <div class="banner">
      <div class="banner-overlay">
        <div class="banner-content">
          <h1>文学殿堂 · 认证大家</h1>
          <p>遇见那些以文字筑梦，以阅读为伴的灵魂</p>
          <el-button v-if="isAdmin" type="primary" size="large" @click="router.push('/verification-review')">
            审核认证申请
          </el-button>
        </div>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 导航栏 -->
      <div class="category-tabs">
        <div class="tabs-container">
          <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="custom-tabs">
            <el-tab-pane
                v-for="(category, index) in categories"
                :key="category"
                :label="category"
                :name="category"
            >
              <template #label>
                <div class="tab-label">
                  <span class="tab-icon">{{categoryIcons[index]}}</span>
                  <span>{{category}}</span>
                </div>
              </template>
              
              <!-- 分类描述 -->
              <div class="category-description">
                <p>{{getCategoryDescription(category)}}</p>
              </div>
              
              <!-- 用户列表 -->
              <div class="user-grid">
                <template v-if="loading">
                  <div v-for="i in 4" :key="i" class="skeleton-card">
                    <el-skeleton animated />
                  </div>
                </template>
                <template v-else>
                  <VUserCard
                      v-for="user in categoryUsers"
                      :key="user.id"
                      :user="user"
                      class="user-card"
                  />
                  <el-empty 
                    v-if="!categoryUsers.length" 
                    description="暂无认证用户，静待佳人到来" 
                    class="empty-state"
                  />
                </template>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getUsersByVerifiedName } from '../../api/user.ts'
import VUserCard from '../../components/VUserCard.vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const categories = ['墨香雅士', '当代鲁迅', '读书达人', '藏书阁主', '笔记大师']
const categoryIcons = ['🖋️', '✒️', '📚', '📖', '📝']
const activeTab = ref(categories[0])
const categoryUsers = ref<any[]>([])
const loading = ref(false)
const isAdmin = computed(() => sessionStorage.getItem('role') === 'admin')

// 获取分类描述
const getCategoryDescription = (category: string): string => {
  const descriptions: Record<string, string> = {
    '墨香雅士': '文字如墨香，在纸上流淌，在心间沉淀。他们以笔墨为伴，以文字为友，在字里行间寻找生活的真谛。',
    '当代鲁迅': '犀利的笔锋，深邃的思想，他们以批判的眼光审视世界，以文字唤醒沉睡的灵魂。',
    '读书达人': '书籍是人类进步的阶梯，他们踏遍千山万水，只为在书海中寻找那份心灵的共鸣。',
    '藏书阁主': '他们的书架如同一座座知识的宝库，珍藏着世间的智慧与美好，等待有缘人的发现。',
    '笔记大师': '阅读不止于眼睛，还在于思考与记录。他们以笔记为媒，与作者对话，与自己交流。'
  }
  return descriptions[category] || '探索阅读的无限可能，发现知识的奇妙世界。'
}

const loadUsers = async (verifiedName: string) => {
  try {
    loading.value = true
    const res = await getUsersByVerifiedName(verifiedName)
    categoryUsers.value = res.data.data || []
  } catch (error) {
    console.error('加载用户失败:', error)
    ElMessage.error('加载用户数据时遇到了一点小问题，请稍后再试')
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
.verification-list {
  position: relative;
  min-height: calc(100vh - 60px);
}

.banner {
  position: relative;
  height: 500px;
  margin-bottom: 30px;
  border-radius: 8px;
  overflow: hidden;
  background-image: url('https://images.unsplash.com/photo-1499257398700-43669759a540?q=80&w=2670&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.banner-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.banner-content {
  text-align: center;
  max-width: 800px;
}

.banner-content h1 {
  font-size: 42px;
  color: #fff;
  margin-bottom: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.banner-content p {
  font-size: 20px;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 25px;
  font-style: italic;
  letter-spacing: 1px;
}

.banner-content :deep(.el-button) {
  background: linear-gradient(135deg, #ff6347 0%, #ff4d29 100%) !important;
  border: none !important;
  color: #fff !important;
}

.banner-content :deep(.el-button):hover {
  background: linear-gradient(135deg, #ff8266 0%, #ff6347 100%) !important;
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(255, 99, 71, 0.25);
}

.main-content {
  padding: 0 10px 40px 10px;
  max-width: 1400px;
  margin: 0 auto;
}

.category-tabs {
  margin-bottom: 20px;
}

.tabs-container {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.custom-tabs {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.custom-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.custom-tabs :deep(.el-tabs__nav-wrap) {
  justify-content: center;
  width: 100%;
  overflow: visible;
}

.custom-tabs :deep(.el-tabs__nav-scroll) {
  display: flex;
  justify-content: center;
  width: 100%;
}

.custom-tabs :deep(.el-tabs__nav) {
  border-radius: 30px;
  background-color: #f8f9fa;
  padding: 5px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-wrap: nowrap;
  width: auto;
}

.custom-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  padding: 0 18px;
  height: 50px;
  line-height: 50px;
  transition: all 0.3s;
  border-radius: 25px;
  white-space: nowrap;
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

.tab-icon {
  font-size: 18px;
}

.category-description {
  text-align: center;
  max-width: 700px;
  margin: 0 auto 30px auto;
  padding: 0 20px;
  font-size: 16px;
  color: #666;
  line-height: 1.8;
  font-style: italic;
  position: relative;
}

.category-description::before,
.category-description::after {
  content: '"';
  position: absolute;
  font-size: 40px;
  color: rgba(255, 99, 71, 0.1);
  font-family: Georgia, serif;
}

.category-description::before {
  top: -20px;
  left: 0;
}

.category-description::after {
  bottom: -40px;
  right: 0;
}

.user-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  padding: 10px 5px;
}

.skeleton-card {
  height: 300px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.user-card {
  transition: all 0.3s;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.user-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.empty-state {
  grid-column: 1 / -1;
  padding: 40px 0;
}

/* 深度修改VUserCard组件样式 */
:deep(.user-card) {
  border-radius: 12px;
  overflow: hidden;
  border: none;
}

:deep(.el-card__body) {
  padding: 0;
}

:deep(.user-avatar) {
  width: 80px;
  height: 80px;
  border: 3px solid white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

:deep(.user-info) {
  padding: 20px;
}

:deep(.user-name) {
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

:deep(.user-badge) {
  background: linear-gradient(135deg, #ff6347 0%, #ff4d29 100%);
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

@media (max-width: 1200px) {
  .user-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 992px) {
  .user-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .banner {
    height: 300px;
  }
  
  .banner-content h1 {
    font-size: 28px;
  }
  
  .banner-content p {
    font-size: 16px;
  }
  
  .main-content {
    padding: 0 10px 20px 10px;
  }
  
  .category-description {
    font-size: 14px;
  }
  
  .user-grid {
    grid-template-columns: repeat(1, 1fr);
    gap: 15px;
  }
  
  .custom-tabs :deep(.el-tabs__item) {
    padding: 0 12px;
    font-size: 13px;
  }
  
  .tab-icon {
    font-size: 16px;
  }
}
</style>
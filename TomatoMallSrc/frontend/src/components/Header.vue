<!-- Header.vue -->
<script setup lang="ts">
import { router } from '../router'
import { User, ShoppingCart, SwitchButton, Menu, Search, Tickets, Medal, ArrowDown } from '@element-plus/icons-vue'
import { ref, onMounted, nextTick } from 'vue'
import { getSearchHistory } from '../api/search'
import { ElMessage } from 'element-plus'

const searchKeyword = ref('')
const showHistory = ref(false)
const searchHistory = ref<Array<{id: number, keyword: string}>>([])
const loadingHistory = ref(false)

// 获取搜索历史
const fetchSearchHistory = async () => {
  try {
    loadingHistory.value = true
    const res = await getSearchHistory()
    if (res.data.code === '200') {
      searchHistory.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取搜索历史失败:', error)
  } finally {
    loadingHistory.value = false
  }
}

// 处理搜索
const handleSearch = () => {
  if (!searchKeyword.value.trim()) return
  showHistory.value = false
  router.push({ path: '/search', query: { keyword: searchKeyword.value.trim() } })
}

// 点击历史记录
const handleHistoryClick = (keyword: string) => {
  searchKeyword.value = keyword
  showHistory.value = false
  router.push({ path: '/search', query: { keyword } })
}

// 处理输入框焦点
const handleFocus = () => {
  if (searchHistory.value.length > 0) {
    showHistory.value = true
  }
}

// 处理输入框失焦
const handleBlur = () => {
  // 延迟隐藏，避免点击历史记录时立即隐藏
  setTimeout(() => {
    showHistory.value = false
  }, 200)
}

// 切换搜索历史显示
const toggleHistory = () => {
  if (searchHistory.value.length > 0) {
    showHistory.value = !showHistory.value
  }
}

// 处理键盘事件
const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter') {
    handleSearch()
  } else if (event.key === 'Escape') {
    showHistory.value = false
  }
}

function logout() {
  sessionStorage.clear()
  router.push({path: "/login"})
}

function goToWelcome() {
  router.push({path: "/welcome"})
}

onMounted(() => {
  fetchSearchHistory()
})
</script>

<template>
  <header class="header1">
    <div class="header1-content">
      <div class="header1-logo" @click="goToWelcome">
        <div class="logo-container">
          <span class="tomato-icon">🍅</span>
          <span class="logo-text">番茄书城</span>
          <span class="book-icon">📚</span>
        </div>
      </div>
      <nav class="header1-nav">
        <el-tooltip content="官方认证大师榜" placement="bottom">
          <el-icon :size="24" class="header1-icon" @click="router.push('/verification-list')">
            <Medal />
          </el-icon>
        </el-tooltip>
        <el-tooltip content="优惠券" placement="bottom">
          <el-icon :size="24" class="header1-icon" @click="router.push('/coupons')">
            <Tickets />
          </el-icon>
        </el-tooltip>
        <el-tooltip content="购物车" placement="bottom">
          <el-icon :size="24" class="header1-icon" @click="router.push('/cart')">
            <ShoppingCart />
          </el-icon>
        </el-tooltip>
        <el-tooltip content="书城" placement="bottom">
          <el-icon :size="24" class="header1-icon" @click="router.push('/productlist')">
            <Menu />
          </el-icon>
        </el-tooltip>
        <el-tooltip content="个人中心" placement="bottom">
          <el-icon :size="24" class="header1-icon" @click="router.push('/dashboard')">
            <User />
          </el-icon>
        </el-tooltip>
        <el-icon :size="24" class="header1-icon" @click="logout">
          <SwitchButton />
        </el-icon>
      </nav>
      <div class="header1-search">
        <div class="search-container">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索商品、用户、书单..."
            class="header1-search-input"
            @keyup.enter="handleSearch"
            @focus="handleFocus"
            @blur="handleBlur"
            @keydown="handleKeydown"
          >
            <template #prefix>
              <el-icon class="search-icon"><Search /></el-icon>
            </template>
            <template #suffix>
              <el-icon 
                v-if="searchHistory.length > 0" 
                class="history-icon"
                :class="{ 'active': showHistory }"
                @click.stop="toggleHistory"
              >
                <ArrowDown />
              </el-icon>
            </template>
          </el-input>
          
          <!-- 搜索历史下拉框 -->
          <div v-if="showHistory && searchHistory.length > 0" class="search-history-dropdown">
            <div class="history-header">
              <span>搜索历史</span>
            </div>
            <div class="history-list">
              <div 
                v-for="item in searchHistory.slice(0, 8)" 
                :key="item.id" 
                class="history-item"
                @click="handleHistoryClick(item.keyword)"
              >
                <el-icon class="history-search-icon"><Search /></el-icon>
                <span class="history-keyword">{{ item.keyword }}</span>
              </div>
            </div>
            <div v-if="searchHistory.length === 0" class="no-history">
              暂无搜索历史
            </div>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header1 {
  width: 100vw;
  position: fixed;
  top: 0;
  left: 0;
  z-index: 100;
  background: linear-gradient(to bottom, rgba(40,40,40,0.85) 0%, rgba(40,40,40,0.15) 100%);
  box-shadow: none;
  border: none;
  height: 72px;
  display: flex;
  align-items: center;
  backdrop-filter: blur(6px);
  opacity: 0.88;
}
.header1-content {
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 48px;
  height: 72px;
}
.header1-logo {
  color: #fff;
  font-size: 32px;
  font-weight: bold;
  letter-spacing: 2px;
  text-shadow: 0 2px 8px rgba(0,0,0,0.25);
  cursor: pointer;
  transition: transform 0.3s;
}
.header1-logo:hover {
  transform: scale(1.05);
}
.logo-container {
  display: flex;
  align-items: center;
  gap: 8px;
}
.logo-text {
  background: linear-gradient(45deg, #ff6347, #ffe066);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  position: relative;
}
.tomato-icon {
  font-size: 28px;
  animation: bounce 2s infinite;
}
.book-icon {
  font-size: 28px;
  animation: float 3s ease-in-out infinite;
}
@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-6px);
  }
  60% {
    transform: translateY(-3px);
  }
}
@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-6px);
  }
}
.header1-nav {
  display: flex;
  gap: 32px;
  align-items: center;
}
.header1-icon {
  color: #fff;
  cursor: pointer;
  font-size: 24px;
  transition: color 0.2s;
}
.header1-icon:hover {
  color: #ffe066;
}
.header1-search {
  min-width: 320px;
  max-width: 400px;
  position: relative;
}
.search-container {
  position: relative;
  width: 100%;
}
.header1-search-input {
  width: 100%;
  border-radius: 24px;
  background: rgba(255,255,255,0.92);
}
.header1-search-input :deep(.el-input__wrapper) {
  border-radius: 24px;
  background: rgba(255,255,255,0.92);
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.08);
}
.search-icon {
  color: #d9534f;
  font-size: 18px;
}
.history-icon {
  color: #909399;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
}
.history-icon.active {
  transform: rotate(180deg);
  color: #d9534f;
}
.search-history-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  border: 1px solid #ebeef5;
  z-index: 1000;
  max-height: 320px;
  overflow: hidden;
  backdrop-filter: blur(10px);
}
.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}
.history-header span {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}
.history-list {
  max-height: 240px;
  overflow-y: auto;
}
.history-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 1px solid #f8f8f8;
}
.history-item:hover {
  background: #f5f7fa;
}
.history-item:last-child {
  border-bottom: none;
}
.history-search-icon {
  color: #909399;
  font-size: 14px;
}
.history-keyword {
  color: #303133;
  font-size: 14px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.no-history {
  padding: 24px 16px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}
/* 自定义滚动条 */
.history-list::-webkit-scrollbar {
  width: 4px;
}
.history-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 2px;
}
.history-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 2px;
}
.history-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>
<!-- Header.vue -->
<script setup lang="ts">
import { router } from '../router'
import { User, ShoppingCart, SwitchButton, Menu, Search } from '@element-plus/icons-vue'
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSearchHistory, type SearchHistoryItem } from '../api/search'

const role = sessionStorage.getItem('role')
console.log("role => ",role)
const username = sessionStorage.getItem('username')

const searchKeyword = ref('')
const showHistory = ref(false)
const searchHistory = ref<SearchHistoryItem[]>([])

// 获取搜索历史
const fetchSearchHistory = async () => {
  try {
    const res = await getSearchHistory()
    if (res.data.code === '200') {
      searchHistory.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取搜索历史失败:', error)
  }
}

// 处理搜索
const handleSearch = () => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  router.push({
    path: '/search',
    query: { keyword: searchKeyword.value.trim() }
  })
  showHistory.value = false
}

// 处理历史记录点击
const handleHistoryClick = (keyword: string) => {
  searchKeyword.value = keyword
  handleSearch()
}

// 处理点击外部关闭历史记录
const handleClickOutside = (event: MouseEvent) => {
  const searchContainer = document.querySelector('.search-container')
  if (searchContainer && !searchContainer.contains(event.target as Node)) {
    showHistory.value = false
  }
}

onMounted(() => {
  fetchSearchHistory()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

function logout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    customClass: 'custom-dialog',
    type: 'warning',
    center: true
  }).then(() => {
    console.log("login")
    sessionStorage.clear()
    router.push({path: "/login"})
  })
}
</script>

<template>
  <el-header class="header-container">
    <div class="header-content">
      <div class="left-section">
        <h1 class="logo-text">番茄书城</h1>
<!--        <el-tag class="role-tag" effect="dark">{{ role === 'admin' ? '管理员模式' : '顾客模式' }}</el-tag>-->
      </div>

      <div class="search-container">
        <div class="search-box">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索商品、用户、书单..."
            class="search-input"
            @keyup.enter="handleSearch"
            @focus="showHistory = true"
          >
            <template #prefix>
              <el-icon class="search-icon"><Search /></el-icon>
            </template>
          </el-input>
        </div>
        
        <!-- 搜索历史 -->
        <div v-if="showHistory && searchHistory.length > 0" class="search-history">
          <div class="history-header">
            <span class="history-title">搜索历史</span>
          </div>
          <div class="history-tags">
            <el-tag
              v-for="item in searchHistory"
              :key="item.id"
              class="history-tag"
              @click="handleHistoryClick(item.keyword)"
            >
              {{ item.keyword }}
            </el-tag>
          </div>
        </div>
      </div>

      <div class="right-section">
        <el-tooltip content="购物车" placement="bottom">
          <el-icon :size="24" class="header-icon" @click="router.push('/cart')">
            <ShoppingCart />
          </el-icon>
        </el-tooltip>
        <el-tooltip content="书城" placement="bottom">
          <el-icon :size="24" class="header-icon" @click="router.push('/productlist')">
            <Menu />
          </el-icon>
        </el-tooltip>
        <el-tooltip content="个人中心" placement="bottom">
          <el-icon :size="24" class="header-icon" @click="router.push('/dashboard')">
            <User />
          </el-icon>
        </el-tooltip>
        <el-icon :size="24" class="header-icon" @click="logout">
          <SwitchButton />
        </el-icon>
      </div>
    </div>
  </el-header>
</template>

<style scoped>
.header-container {
  background-color: #2c698d;
  height: 60px;
  display: flex;
  align-items: center;
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30px;
}

.logo-text {
  color: #ffffff;
  margin: 0;
  font-size: 24px;
  letter-spacing: 2px;
}

.role-tag {
  margin-left: 15px;
  background-color: #bae8e8;
  color: #2c698d;
  border: none;
}

.right-section {
  display: flex;
  align-items: center;
  gap: 25px;
}

.user-info {
  display: flex;
  align-items: center;
  color: #ffffff;
  cursor: pointer;
  .username {
    margin-left: 8px;
    font-size: 14px;
  }
}

.header-icon {
  color: #ffffff;
  cursor: pointer;
  transition: color 0.3s;
  &:hover {
    color: #e3f6f5;
  }
}

.search-container {
  position: relative;
  flex: 1;
  max-width: 600px;
  margin: 0 20px;
}

.search-box {
  width: 100%;
}

.search-input {
  width: 100%;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  background-color: rgba(255, 255, 255, 0.9);
}

.search-icon {
  color: #2c698d;
  font-size: 18px;
}

.search-history {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 12px;
  margin-top: 5px;
  z-index: 1000;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.history-title {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-tag {
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 4px;
}

.history-tag:hover {
  transform: translateY(-2px);
  background-color: #ecf5ff;
  border-color: #409EFF;
  color: #409EFF;
}
</style>
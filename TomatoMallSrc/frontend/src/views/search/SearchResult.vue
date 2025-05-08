<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElLoading, ElDialog } from 'element-plus'
import { search, type SearchResult } from '../../api/search'
import Header from '../../components/Header.vue'
import BookListItem from '../../components/BookListItem.vue'
import { collectBookList, cancelCollectBookList, deleteBookList } from '../../api/booklist'

const route = useRoute()
const router = useRouter()
const searchResults = ref<SearchResult>({
  accounts: [],
  products: [],
  bookLists: []
})
const loading = ref(false)
const activeTab = ref('products')
const currentUserId = ref<number | null>(null)
const favouriteBookListIds = ref<Set<number>>(new Set())

// 书单详情相关
const detailDialogVisible = ref(false)
const currentBookList = ref<any>(null)

const fetchSearchResults = async () => {
  const keyword = route.query.keyword as string
  if (!keyword) {
    router.push('/')
    return
  }

  loading.value = true
  try {
    const res = await search(keyword)
    if (res.data.code === '200') {
      // 确保每个书单都有 products 数组
      searchResults.value = {
        ...res.data.data,
        bookLists: res.data.data.bookLists.map((bookList: any) => ({
          ...bookList,
          products: bookList.products || []
        }))
      }
    }
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

// 处理商品点击
const handleProductClick = (productId: string) => {
  router.push(`/product/${productId}`)
}

// 处理用户点击
const handleUserClick = (userId: number) => {
  router.push(`/user/${userId}`)
}

// 处理书单收藏
const handleCollect = async (bookList: any) => {
  try {
    const isCollected = favouriteBookListIds.value.has(bookList.id)
    if (isCollected) {
      await cancelCollectBookList({ bookListId: bookList.id })
      bookList.favouriteCount--
      favouriteBookListIds.value.delete(bookList.id)
      ElMessage.success('取消收藏成功')
    } else {
      await collectBookList({ bookListId: bookList.id })
      bookList.favouriteCount++
      favouriteBookListIds.value.add(bookList.id)
      ElMessage.success('收藏成功')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 处理书单删除
const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个书单吗？',
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const loading = ElLoading.service({
      lock: true,
      text: '正在删除书单...',
      background: 'rgba(0, 0, 0, 0.7)'
    })

    try {
      await deleteBookList(id)
      ElMessage.success('删除书单成功')
      // 从搜索结果中移除已删除的书单
      searchResults.value.bookLists = searchResults.value.bookLists.filter(list => list.id !== id)
    } catch (error) {
      ElMessage.error('删除书单失败，请重试')
    } finally {
      loading.close()
    }
  } catch {
    // 用户取消删除操作
  }
}

// 处理查看书单详情
const handleView = (bookList: any) => {
  currentBookList.value = bookList
  detailDialogVisible.value = true
}

onMounted(async () => {
  // 获取当前用户ID
  const userId = sessionStorage.getItem('userId')
  if (userId) {
    currentUserId.value = Number(userId)
  }
  await fetchSearchResults()
})
</script>

<template>
  <div class="search-result-page">
    <Header />
    <div class="search-result-container" v-loading="loading">
      <h2 class="search-title">搜索结果: {{ route.query.keyword }}</h2>
      
      <!-- 导航栏 -->
      <div class="search-tabs">
        <div 
          v-for="tab in [
            { key: 'products', label: '商品', count: searchResults.products.length },
            { key: 'accounts', label: '用户', count: searchResults.accounts.length },
            { key: 'bookLists', label: '书单', count: searchResults.bookLists.length }
          ]" 
          :key="tab.key"
          :class="['tab-item', { active: activeTab === tab.key }]"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
          <span class="count">({{ tab.count }})</span>
        </div>
      </div>
      
      <!-- 商品结果 -->
      <div v-show="activeTab === 'products'" class="result-content">
        <div class="product-grid">
          <div 
            v-for="product in searchResults.products" 
            :key="product.id" 
            class="product-card"
            @click="handleProductClick(product.id)"
          >
            <img :src="product.cover" :alt="product.title" class="product-cover">
            <div class="product-info">
              <h4>{{ product.title }}</h4>
              <p class="price">¥{{ product.price }}</p>
              <p class="description">{{ product.description }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 用户结果 -->
      <div v-show="activeTab === 'accounts'" class="result-content">
        <div class="user-grid">
          <div 
            v-for="account in searchResults.accounts" 
            :key="account.id" 
            class="user-card"
            @click="handleUserClick(account.id)"
          >
            <img :src="account.avatar" :alt="account.username" class="user-avatar">
            <div class="user-info">
              <h4>{{ account.username }}</h4>
              <p>关注者: {{ account.followerCount }}</p>
              <p>关注中: {{ account.followingCount }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 书单结果 -->
      <div v-show="activeTab === 'bookLists'" class="result-content">
        <div class="booklist-grid">
          <book-list-item
            v-for="bookList in searchResults.bookLists"
            :key="bookList.id"
            :book-list="bookList"
            :is-favourite="favouriteBookListIds.has(bookList.id)"
            :is-creator="currentUserId === bookList.creatorId"
            @collect="handleCollect"
            @delete="handleDelete"
            @view="handleView"
          />
        </div>
      </div>

      <!-- 书单详情对话框 -->
      <el-dialog
        v-model="detailDialogVisible"
        title="书单详情"
        width="800px"
      >
        <div v-if="currentBookList" class="booklist-detail">
          <h2>{{ currentBookList.title }}</h2>
          <p class="description">{{ currentBookList.description }}</p>
          
          <div class="products-list">
            <div v-for="product in currentBookList.products" :key="product.id" class="product-item">
              <img :src="product.cover" :alt="product.title" class="product-cover">
              <div class="product-info">
                <h4>{{ product.title }}</h4>
                <p class="price">¥{{ product.price }}</p>
              </div>
            </div>
          </div>
        </div>
      </el-dialog>

      <!-- 无结果提示 -->
      <div v-if="!loading && 
                searchResults.products.length === 0 && 
                searchResults.accounts.length === 0 && 
                searchResults.bookLists.length === 0" 
           class="no-results">
        未找到相关结果
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-result-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.search-result-container {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.search-title {
  font-size: 24px;
  color: #303133;
  margin-bottom: 24px;
  font-weight: 500;
}

.search-tabs {
  display: flex;
  gap: 32px;
  margin-bottom: 24px;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 12px;
}

.tab-item {
  padding: 8px 4px;
  cursor: pointer;
  transition: all 0.3s;
  color: #606266;
  font-size: 16px;
  position: relative;
}

.tab-item:hover {
  color: #409eff;
}

.tab-item.active {
  color: #409eff;
  font-weight: 500;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -13px;
  left: 0;
  width: 100%;
  height: 2px;
  background-color: #409eff;
}

.count {
  font-size: 14px;
  margin-left: 4px;
  opacity: 0.8;
}

.result-content {
  padding: 16px 0;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 24px;
}

.product-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s;
  cursor: pointer;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.product-cover {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.product-info {
  padding: 16px;
}

.product-info h4 {
  margin: 0;
  color: #303133;
  font-size: 16px;
  line-height: 1.4;
  height: 44px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.price {
  color: #f56c6c;
  font-weight: bold;
  margin: 12px 0;
  font-size: 18px;
}

.description {
  color: #606266;
  font-size: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
  height: 40px;
}

.user-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 24px;
}

.user-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.user-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.user-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
}

.user-info h4 {
  margin: 0;
  color: #303133;
  font-size: 16px;
  font-weight: 500;
}

.user-info p {
  margin: 6px 0;
  color: #606266;
  font-size: 14px;
}

.booklist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
}

.no-results {
  text-align: center;
  padding: 48px;
  color: #909399;
  font-size: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

/* 书单详情样式 */
.booklist-detail {
  padding: 20px;
}

.products-list {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.product-item {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.product-cover {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 4px;
}

.product-info {
  flex: 1;
}

.product-info h4 {
  margin: 0;
  font-size: 14px;
  color: #2c3e50;
}

.price {
  color: #f56c6c;
  font-weight: bold;
  margin: 4px 0;
}
</style> 
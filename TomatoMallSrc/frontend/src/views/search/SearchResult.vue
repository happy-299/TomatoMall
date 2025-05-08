<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { search, type SearchResult } from '../../api/search'
import Header from '../../components/Header.vue'

const route = useRoute()
const router = useRouter()
const searchResults = ref<SearchResult>({
  accounts: [],
  products: [],
  bookLists: []
})
const loading = ref(false)

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
      searchResults.value = res.data.data
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

// 处理书单点击
const handleBookListClick = (bookListId: number) => {
  router.push(`/booklist/${bookListId}`)
}

onMounted(() => {
  fetchSearchResults()
})
</script>

<template>
  <div class="search-result-page">
    <Header />
    <div class="search-result-container" v-loading="loading">
      <h2>搜索结果: {{ route.query.keyword }}</h2>
      
      <!-- 商品结果 -->
      <div v-if="searchResults.products.length > 0" class="result-section">
        <h3>商品</h3>
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
      <div v-if="searchResults.accounts.length > 0" class="result-section">
        <h3>用户</h3>
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
      <div v-if="searchResults.bookLists.length > 0" class="result-section">
        <h3>书单</h3>
        <div class="booklist-grid">
          <div 
            v-for="bookList in searchResults.bookLists" 
            :key="bookList.id" 
            class="booklist-card"
            @click="handleBookListClick(bookList.id)"
          >
            <h4>{{ bookList.title }}</h4>
            <p class="description">{{ bookList.description }}</p>
            <div class="booklist-footer">
              <span>{{ bookList.products?.length || 0 }} 本书</span>
              <span>{{ bookList.favouriteCount || 0 }} 收藏</span>
            </div>
          </div>
        </div>
      </div>

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
  background: linear-gradient(120deg, #e3f6f5 0%, #d0eeff 100%);
}

.search-result-container {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.result-section {
  margin-bottom: 32px;
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.result-section h3 {
  margin-bottom: 16px;
  color: #2c3e50;
  font-size: 20px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
}

.product-card {
  background: #f8f9fa;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s;
  cursor: pointer;
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.product-cover {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.product-info {
  padding: 12px;
}

.product-info h4 {
  margin: 0;
  color: #2c3e50;
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
  margin: 8px 0;
  font-size: 18px;
}

.description {
  color: #666;
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
  gap: 20px;
}

.user-card {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.user-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.user-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  object-fit: cover;
}

.user-info h4 {
  margin: 0;
  color: #2c3e50;
  font-size: 16px;
}

.user-info p {
  margin: 4px 0;
  color: #666;
  font-size: 14px;
}

.booklist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.booklist-card {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.booklist-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.booklist-card h4 {
  margin: 0;
  color: #2c3e50;
  font-size: 16px;
  line-height: 1.4;
  height: 44px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.booklist-footer {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  color: #666;
  font-size: 14px;
}

.no-results {
  text-align: center;
  padding: 40px;
  color: #666;
  font-size: 16px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
</style> 
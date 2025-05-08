<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getBookListDetail, type BookListVO } from '../../api/booklist'
import Header from '../../components/Header.vue'

const route = useRoute()
const router = useRouter()
const bookList = ref<BookListVO | null>(null)
const loading = ref(false)

const fetchBookListDetail = async () => {
  const bookListId = route.params.id
  if (!bookListId) {
    router.push('/')
    return
  }

  loading.value = true
  try {
    const res = await getBookListDetail(Number(bookListId))
    if (res.data.code === '200') {
      bookList.value = res.data.data
    }
  } catch (error) {
    ElMessage.error('获取书单详情失败')
  } finally {
    loading.value = false
  }
}

// 处理商品点击
const handleProductClick = (productId: string) => {
  router.push(`/product/${productId}`)
}

// 处理创建者点击
const handleCreatorClick = (creatorId: number) => {
  router.push(`/user/${creatorId}`)
}

onMounted(() => {
  fetchBookListDetail()
})
</script>

<template>
  <div class="booklist-detail-page">
    <Header />
    <div class="booklist-detail-container" v-loading="loading">
      <div v-if="bookList" class="booklist-content">
        <!-- 书单头部信息 -->
        <div class="booklist-header">
          <h1>{{ bookList.title }}</h1>
          <div class="booklist-meta">
            <div class="creator-info" @click="handleCreatorClick(bookList.creatorId)">
              <el-avatar :size="40" :src="bookList.creatorAvatar" />
              <span>{{ bookList.creatorName }}</span>
            </div>
            <div class="booklist-stats">
              <span class="stat-item">
                <i class="el-icon-document"></i>
                {{ bookList.products?.length || 0 }} 本书
              </span>
              <span class="stat-item">
                <i class="el-icon-star-on"></i>
                {{ bookList.favouriteCount }} 收藏
              </span>
              <span class="stat-item">
                <i class="el-icon-time"></i>
                {{ new Date(bookList.creationDate).toLocaleDateString() }}
              </span>
            </div>
          </div>
          <p class="description">{{ bookList.description }}</p>
        </div>

        <!-- 书单内容 -->
        <div class="booklist-products">
          <h2>书单内容</h2>
          <div class="products-grid">
            <div 
              v-for="product in bookList.products" 
              :key="product.id"
              class="product-card"
              @click="handleProductClick(product.id)"
            >
              <img :src="product.cover" :alt="product.title" class="product-cover">
              <div class="product-info">
                <h3>{{ product.title }}</h3>
                <p class="price">¥{{ product.price }}</p>
                <p class="description">{{ product.description }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 无数据提示 -->
      <div v-if="!loading && !bookList" class="no-data">
        书单不存在或已被删除
      </div>
    </div>
  </div>
</template>

<style scoped>
.booklist-detail-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.booklist-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

.booklist-content {
  background: white;
  border-radius: 16px;
  padding: 2rem;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.booklist-header {
  margin-bottom: 2rem;
}

.booklist-header h1 {
  color: #2c698d;
  font-size: 2rem;
  margin: 0 0 1rem 0;
}

.booklist-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.creator-info {
  display: flex;
  align-items: center;
  gap: 1rem;
  cursor: pointer;
  transition: all 0.3s;
}

.creator-info:hover {
  opacity: 0.8;
}

.creator-info span {
  color: #2c698d;
  font-size: 1.1rem;
}

.booklist-stats {
  display: flex;
  gap: 1.5rem;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #606266;
  font-size: 0.9rem;
}

.description {
  color: #606266;
  font-size: 1rem;
  line-height: 1.6;
  margin: 0;
}

.booklist-products {
  margin-top: 2rem;
}

.booklist-products h2 {
  color: #2c698d;
  font-size: 1.5rem;
  margin: 0 0 1.5rem 0;
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 1.5rem;
}

.product-card {
  background: #f8f9fa;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.product-cover {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.product-info {
  padding: 1rem;
}

.product-info h3 {
  margin: 0 0 0.5rem 0;
  color: #2c698d;
  font-size: 1.1rem;
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
  margin: 0.5rem 0;
  font-size: 1.1rem;
}

.description {
  color: #606266;
  font-size: 0.9rem;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
  height: 40px;
}

.no-data {
  text-align: center;
  padding: 3rem;
  color: #909399;
  font-size: 1rem;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}
</style> 
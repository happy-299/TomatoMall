<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElCard, ElMessage, ElButton, ElRate, ElMessageBox } from 'element-plus'
import { getProductById, deleteProduct, type Product } from '../../api/product'

const route = useRoute()
const router = useRouter()
const product = ref<Product | null>(null)
const loading = ref(true)
const isAdmin = ref(!!sessionStorage.getItem('token'))

// 获取商品详情
const fetchProduct = async () => {
  try {
    const res = await getProductById(route.params.id as string);
    console.log('完整响应:', res);

    // 修正数据结构解析
    if (res.data && res.data.code === "200") {
      product.value = res.data.data; // 正确访问数据层级
    } else {
      throw new Error(res.data?.msg || '无效的响应结构');
    }
  } catch (error: any) {
    console.error('请求错误:', error);
    ElMessage.error(`加载失败: ${error.message}`);
  } finally {
    loading.value = false; // 确保关闭加载状态
  }
};

// 删除商品
const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除该商品吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteProduct(route.params.id as string)
    ElMessage.success('删除成功')
    router.push('/productList')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  fetchProduct()
})
</script>

<template>
  <div class="product-detail-container">
    <el-card v-loading="loading" class="product-card">

      <!-- 管理员操作 -->
      <div v-if="isAdmin" class="admin-actions">
        <el-button
            type="danger"
            @click.stop="handleDelete"
        >
          删除商品
        </el-button>
      </div>

      <!-- 商品内容 -->
      <div v-if="product" class="product-content">
        <!-- 封面大图 -->
        <div class="cover-container">
          <img
              v-if="product.cover"
              :src="product.cover"
              class="product-cover"
              @error="(e: any) => e.target.style.display = 'none'"
          >
          <div v-else class="cover-placeholder">暂无图片</div>
        </div>

        <!-- 商品信息 -->
        <div class="product-info">
          <h1 class="title">{{ product.title }}</h1>

          <div class="meta-section">
            <div class="price-rate">
              <span class="price">¥{{ product.price.toFixed(2) }}</span>
              <el-rate
                  v-model="product.rate"
                  :max="10"
                  disabled
                  :colors="['#272643', '#272643', '#272643']"
              />
            </div>

            <!-- 商品规格 -->
            <div v-if="product.specifications?.length" class="specifications">
              <h3>产品规格</h3>
              <div class="spec-grid">
                <div
                    v-for="spec in product.specifications"
                    :key="spec.id"
                    class="spec-item"
                >
                  <span class="spec-label">{{ spec.item }}：</span>
                  <el-tag type="info" class="spec-value">{{ spec.value }}</el-tag>
                </div>
              </div>
            </div>

            <!-- 商品描述和详情 -->
            <div class="description-section">
              <div class="description">
                <h3>商品描述</h3>
                <p>{{ product.description || '暂无详细描述' }}</p>
              </div>

              <div v-if="product.detail" class="detail">
                <h3>商品详情</h3>
                <p class="detail-content">{{ product.detail }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.product-detail-container {
  padding: 24px;
  background: linear-gradient(120deg, #e3f6f5 0%, #d0eeff 100%);
  min-height: 100vh;
}

.product-card {
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
  border-radius: 12px;
}

.back-button {
  position: absolute;
  left: 20px;
  top: 20px;
  z-index: 1;
}

.admin-actions {
  position: absolute;
  right: 20px;
  top: 20px;
  z-index: 1;
}

.product-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  padding: 24px;
}

.cover-container {
  position: relative;
  height: 0;
  padding-bottom: 100%;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fa;
}

.product-cover {
  position: absolute;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  position: absolute;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 16px;
  background: #f0f2f5;
}

.product-info {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.title {
  color: #272643;
  font-size: 28px;
  margin: 0;
}

.price-rate {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.price {
  color: #ff4d4f;
  font-size: 32px;
  font-weight: bold;
}

.description {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
}

.description h3 {
  color: #272643;
  margin: 0 0 12px 0;
  font-size: 18px;
}

.description p {
  color: #606266;
  line-height: 1.6;
  margin: 0;
}

@media (max-width: 768px) {
  .product-content {
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .title {
    font-size: 24px;
  }

  .price {
    font-size: 28px;
  }
}
</style>
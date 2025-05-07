<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElDialog, ElForm, ElFormItem, ElInput, ElButton, ElPagination, ElSelect, ElOption } from 'element-plus'
import { Star, StarFilled, Plus, Delete, ShoppingCart } from '@element-plus/icons-vue'
import {
  BookListVO,
  getAllBookLists,
  createBookList,
  deleteBookList,
  collectBookList,
  cancelCollectBookList,
  getMyBookLists,
  getFavouriteBookLists,
  addItemToBookList,
  removeItemFromBookList,
  type BookListCreateDTO
} from '../../api/booklist'
import { getUserInfo } from '../../api/user'
import { getProducts, type Product } from '../../api/product'

const bookLists = ref<BookListVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const isAdmin = ref(false)
const loading = ref(false)
const currentUserId = ref<number | null>(null)
const favouriteBookListIds = ref<Set<number>>(new Set())

// 创建书单相关
const createDialogVisible = ref(false)
const createForm = ref<BookListCreateDTO>({
  title: '',
  description: '',
  productIds: []
})

// 书单详情相关
const detailDialogVisible = ref(false)
const currentBookList = ref<BookListVO | null>(null)

// 商品选择相关
const products = ref<Product[]>([])
const selectedProducts = ref<number[]>([])

// 获取收藏的书单ID列表
const fetchFavouriteBookListIds = async () => {
  try {
    const res = await getFavouriteBookLists(0, 1000)
    if (res.data.data) {
      favouriteBookListIds.value = new Set(res.data.data.content.map(book => book.id))
    }
  } catch (error) {
    console.error('获取收藏书单失败:', error)
  }
}

// 获取所有书单
const fetchBookLists = async () => {
  loading.value = true
  try {
    const res = await getAllBookLists(currentPage.value - 1, pageSize.value)
    if (res.data.data) {
      bookLists.value = res.data.data.content
      total.value = res.data.data.total
      // 更新收藏状态
      await fetchFavouriteBookListIds()
    }
  } catch (error) {
    ElMessage.error('获取书单列表失败')
  } finally {
    loading.value = false
  }
}

// 获取所有商品
const fetchProducts = async () => {
  try {
    const res = await getProducts()
    products.value = res.data.data
  } catch (error) {
    ElMessage.error('获取商品列表失败')
  }
}

// 创建书单
const handleCreate = async () => {
  try {
    await createBookList(createForm.value)
    ElMessage.success('创建书单成功')
    createDialogVisible.value = false
    createForm.value = {
      title: '',
      description: '',
      productIds: []
    }
    await fetchBookLists()
  } catch (error) {
    ElMessage.error('创建书单失败')
  }
}

// 删除书单
const handleDelete = async (id: number) => {
  try {
    await deleteBookList(id)
    ElMessage.success('删除书单成功')
    await fetchBookLists()
  } catch (error) {
    ElMessage.error('删除书单失败')
  }
}

// 收藏/取消收藏书单
const handleCollect = async (bookList: BookListVO) => {
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
    // 刷新当前页面的数据
    await handleTabChange(activeTab.value)
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 切换标签页
const activeTab = ref('all')
const handleTabChange = async (tab: string) => {
  activeTab.value = tab
  loading.value = true
  try {
    let res
    switch (tab) {
      case 'all':
        res = await getAllBookLists(currentPage.value - 1, pageSize.value)
        break
      case 'mine':
        res = await getMyBookLists(currentPage.value - 1, pageSize.value)
        break
      case 'favourites':
        res = await getFavouriteBookLists(currentPage.value - 1, pageSize.value)
        break
    }
    if (res && res.data.data) {
      bookLists.value = res.data.data.content
      total.value = res.data.data.total
      // 更新收藏状态
      await fetchFavouriteBookListIds()
    }
  } catch (error) {
    ElMessage.error('获取书单列表失败')
  } finally {
    loading.value = false
  }
}

// 页码改变
const handlePageChange = (page: number) => {
  currentPage.value = page
  handleTabChange(activeTab.value)
}

// 每页条数改变
const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1 // 切换每页条数时重置到第一页
  handleTabChange(activeTab.value)
}

// 查看书单详情
const handleViewDetail = (bookList: BookListVO) => {
  currentBookList.value = bookList
  detailDialogVisible.value = true
}

// 添加商品到书单
const handleAddProduct = async (bookListId: number, productId: number) => {
  try {
    await addItemToBookList(bookListId, productId)
    ElMessage.success('添加商品成功')
    await handleTabChange(activeTab.value)
  } catch (error) {
    ElMessage.error('添加商品失败')
  }
}

// 从书单移除商品
const handleRemoveProduct = async (bookListId: number, productId: number) => {
  try {
    await removeItemFromBookList(bookListId, productId)
    ElMessage.success('移除商品成功')
    await handleTabChange(activeTab.value)
  } catch (error) {
    ElMessage.error('移除商品失败')
  }
}

onMounted(async () => {
  // 获取用户信息
  try {
    const username = sessionStorage.getItem('username')
    if (username) {
      const res = await getUserInfo(username)
      isAdmin.value = res.data.data?.role === 'admin'
      currentUserId.value = res.data.data?.id
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
  
  await Promise.all([fetchBookLists(), fetchProducts()])
})
</script>

<template>
  <div class="booklist-container">
    <!-- 头部 -->
    <div class="header">
      <h1>书单列表</h1>
      <el-button type="primary" @click="createDialogVisible = true">
        <el-icon><Plus /></el-icon>
        创建书单
      </el-button>
    </div>

    <!-- 标签页 -->
    <div class="tabs">
      <el-button
        :type="activeTab === 'all' ? 'primary' : 'default'"
        @click="handleTabChange('all')"
      >
        全部书单
      </el-button>
      <el-button
        :type="activeTab === 'mine' ? 'primary' : 'default'"
        @click="handleTabChange('mine')"
      >
        我的书单
      </el-button>
      <el-button
        :type="activeTab === 'favourites' ? 'primary' : 'default'"
        @click="handleTabChange('favourites')"
      >
        收藏的书单
      </el-button>
    </div>

    <!-- 书单列表 -->
    <div class="booklist-grid" v-loading="loading">
      <div v-for="bookList in bookLists" :key="bookList.id" class="booklist-card">
        <div class="booklist-header">
          <h3 @click="handleViewDetail(bookList)" class="clickable">{{ bookList.title }}</h3>
          <div class="actions">
            <el-button
              :type="favouriteBookListIds.has(bookList.id) ? 'warning' : 'default'"
              circle
              @click="handleCollect(bookList)"
            >
              <el-icon>
                <StarFilled v-if="favouriteBookListIds.has(bookList.id)" />
                <Star v-else />
              </el-icon>
            </el-button>
            <el-button
              v-if="currentUserId === bookList.creatorId"
              type="danger"
              circle
              @click="handleDelete(bookList.id)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
        <p class="description">{{ bookList.description }}</p>
        <div class="booklist-footer">
          <div class="creator">
            <el-avatar :size="24" :src="bookList.creatorAvatar" />
            <span>{{ bookList.creatorName }}</span>
          </div>
          <div class="stats">
            <span>{{ bookList.products.length }} 本书</span>
            <span>{{ bookList.favouriteCount }} 收藏</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 创建书单对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      title="创建书单"
      width="500px"
    >
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="createForm.title" placeholder="请输入书单标题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入书单描述"
          />
        </el-form-item>
        <el-form-item label="商品">
          <el-select
            v-model="createForm.productIds"
            multiple
            filterable
            placeholder="请选择商品"
            style="width: 100%"
          >
            <el-option
              v-for="product in products"
              :key="product.id"
              :label="product.title"
              :value="product.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

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
            <div class="product-actions">
              <el-button
                v-if="currentUserId === currentBookList.creatorId"
                type="danger"
                circle
                @click="handleRemoveProduct(currentBookList.id, product.id)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </div>

        <div v-if="currentUserId === currentBookList.creatorId" class="add-product">
          <el-select
            v-model="selectedProducts"
            multiple
            filterable
            placeholder="添加商品到书单"
            style="width: 100%"
          >
            <el-option
              v-for="product in products"
              :key="product.id"
              :label="product.title"
              :value="product.id"
            />
          </el-select>
          <el-button type="primary" @click="handleAddProduct(currentBookList.id, selectedProducts[0])">
            添加商品
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.booklist-container {
  padding: 24px;
  background: linear-gradient(120deg, #e3f6f5 0%, #d0eeff 100%);
  min-height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.tabs {
  margin-bottom: 24px;
  display: flex;
  gap: 16px;
}

.booklist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.booklist-card {
  background: white;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: transform 0.3s;
}

.booklist-card:hover {
  transform: translateY(-5px);
}

.booklist-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.booklist-header h3 {
  margin: 0;
  font-size: 18px;
  color: #2c3e50;
}

.actions {
  display: flex;
  gap: 8px;
}

.description {
  color: #666;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.booklist-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #666;
}

.creator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stats {
  display: flex;
  gap: 16px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 20px;
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.pagination :deep(.el-pagination) {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 16px;
  border-radius: 8px;
  background: #f8f9fa;
}

.pagination :deep(.el-pagination .el-pagination__total) {
  margin-right: 16px;
  font-weight: 500;
  color: #606266;
}

.pagination :deep(.el-pagination .el-pagination__sizes) {
  margin-right: 16px;
}

.pagination :deep(.el-pagination .el-select .el-input) {
  width: 110px;
}

.pagination :deep(.el-pagination .el-pagination__sizes .el-input__inner) {
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  background-color: white;
}

.pagination :deep(.el-pagination .btn-prev),
.pagination :deep(.el-pagination .btn-next) {
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 0 12px;
  height: 32px;
  line-height: 32px;
  transition: all 0.3s;
}

.pagination :deep(.el-pagination .btn-prev:hover),
.pagination :deep(.el-pagination .btn-next:hover) {
  color: #409EFF;
  border-color: #409EFF;
  background-color: #ecf5ff;
}

.pagination :deep(.el-pagination .el-pager li) {
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  transition: all 0.3s;
}

.pagination :deep(.el-pagination .el-pager li:hover) {
  color: #409EFF;
  border-color: #409EFF;
  background-color: #ecf5ff;
}

.pagination :deep(.el-pagination .el-pager li.active) {
  background-color: #409EFF;
  color: white;
  border-color: #409EFF;
  font-weight: bold;
}

.pagination :deep(.el-pagination .el-pager li.active:hover) {
  background-color: #66b1ff;
  border-color: #66b1ff;
}

.clickable {
  cursor: pointer;
}

.clickable:hover {
  color: #409EFF;
}

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

.product-actions {
  display: flex;
  justify-content: flex-end;
}

.add-product {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}
</style> 
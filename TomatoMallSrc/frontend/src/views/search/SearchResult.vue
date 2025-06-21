<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Star } from '@element-plus/icons-vue'
import {  reactive} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage, ElMessageBox, ElLoading} from 'element-plus'
import {search, type SearchResult} from '../../api/search'
import Header from '../../components/Header.vue'
import BookListItem from '../../components/BookListItem.vue'
import ProductCard from '../../components/ProductCard.vue'
import {
  collectBookList,
  cancelCollectBookList,
  deleteBookList,
  getAllBookLists,
  type BookListVO
} from '../../api/booklist'
import {deleteNote, getNoteLikeStatus, getNotePayStatus, payNote, type NoteVO, updateNote, likeNote, unlikeNote} from '../../api/note'
import ReadingNote from "../../components/ReadingNote.vue";
import {uploadUserImage} from '../../api/util.ts'
import VUserCard from '../../components/VUserCard.vue'
import { getStockpile } from '../../api/product'
import { getCart } from '../../api/cart'

const route = useRoute()
const router = useRouter()
const searchResults = ref<SearchResult>({
  accounts: [],
  products: [],
  bookLists: [],
  notes: []
})
const loading = ref(false)
const activeTab = ref('products')
const currentUserId = ref<number | null>(null)
const favouriteBookListIds = ref<Set<number>>(new Set())
const likedNoteIds = ref<Set<number>>(new Set())
const paidNoteIds = ref<Set<number>>(new Set())

// 书单详情相关
const detailDialogVisible = ref(false)
const currentBookList = ref<BookListVO | null>(null)
const detailLoading = ref(false)

// 笔记相关状态
const showPurchaseDialog = ref(false)
const selectedNote = ref<NoteVO | null>(null)
const detailNoteDialogVisible = ref(false)
const currentNote = ref<NoteVO | null>(null)
const editNoteDialogVisible = ref(false)
const editNoteForm = reactive({
  id: -1,
  title: '',
  content: '',
  price: 0,
  img: ''
})

// ProductCard相关状态
const stockpiles = ref<Record<string, any>>({})
const cartItems = ref<Record<string, any>>({})
const isAdmin = ref(false)

const fetchSearchResults = async () => {
  const keyword = route.query.keyword as string
  if (!keyword) {
    router.push('/')
    return
  }

  loading.value = true
  try {
    const res = await search(keyword)
    console.log("search => ", res)
    if (res.data.code === '200') {
      // 获取完整的书单信息
      const bookListRes = await getAllBookLists(0, 1000)
      const fullBookLists = bookListRes.data.data.content

      // 更新搜索结果中的书单信息
      searchResults.value = {
        ...res.data.data,
        bookLists: res.data.data.bookLists.map((bookList: BookListVO) => {
          // 查找完整的书单信息
          const fullBookList = fullBookLists.find(list => list.id === bookList.id)
          if (fullBookList) {
            return {
              ...bookList,
              products: fullBookList.products || []
            }
          }
          return {
            ...bookList,
            products: []
          }
        })
      }

      // 获取商品库存信息
      await Promise.all(searchResults.value.products.map(async (product) => {
        try {
          const stockRes = await getStockpile(product.id)
          stockpiles.value[product.id] = stockRes.data.data || { amount: 0, frozen: 0 }
        } catch (error) {
          console.error(`获取商品 ${product.id} 库存失败:`, error)
          stockpiles.value[product.id] = { amount: 0, frozen: 0 }
        }
      }))

      // 获取购物车信息
      try {
        const cartRes = await getCart()
        const cartItemsData = cartRes.data.data.items || []
        cartItems.value = {}
        cartItemsData.forEach((item: any) => {
          cartItems.value[item.productId] = item
        })
      } catch (error) {
        console.error('获取购物车失败:', error)
      }

      // 获取笔记状态
      const notePromises = searchResults.value.notes.map(async (note: NoteVO) => {
        if (currentUserId.value) {
          try {
            const [likeRes, payRes] = await Promise.all([
              getNoteLikeStatus(note.id),
              getNotePayStatus(note.id)
            ])
            if (likeRes.data.data) likedNoteIds.value.add(note.id)
            if (payRes.data.data) paidNoteIds.value.add(note.id)
          } catch (error) {
            console.error('获取笔记状态失败', error)
          }
        }
      })

      await Promise.all(notePromises)
    }
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

// 笔记点赞处理
const handleLikeNote = async (note: NoteVO) => {
  if (!currentUserId.value) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await likeNote(note.id)
    likedNoteIds.value.add(note.id)
    note.likeCnt++
  } catch (error) {
    ElMessage.error('点赞失败')
  }
}

const handleUnlikeNote = async (note: NoteVO) => {
  try {
    await unlikeNote(note.id)
    likedNoteIds.value.delete(note.id)
    note.likeCnt--
  } catch (error) {
    ElMessage.error('取消点赞失败')
  }
}

// 删除笔记
const handleDeleteNote = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定删除该笔记？', '提示', {type: 'warning'})
    await deleteNote(id)
    searchResults.value.notes = searchResults.value.notes.filter(n => n.id !== id)
    ElMessage.success('删除成功')
  } catch (error) {
    // 取消操作
  }
}

// 购买笔记处理
const handlePurchaseNote = (note: NoteVO) => {
  selectedNote.value = note
  showPurchaseDialog.value = true
}

const getDisplayContent = (content: string, isPaid: boolean) => {
  if (isPaid || !content) return content
  const showLength = Math.ceil(content.length * 0.35)
  return content.slice(0, showLength) + '...'
}

const confirmPurchase = async () => {
  if (!selectedNote.value) return

  const loading = ElLoading.service({
    lock: true,
    text: '正在购买...',
    background: 'rgba(0, 0, 0, 0.7)'
  })

  try {
    await payNote(selectedNote.value.id)
    paidNoteIds.value.add(selectedNote.value.id)
    ElMessage.success('购买成功')
    showPurchaseDialog.value = false
  } catch (error: any) {
    const errorMessage = error.response?.data?.msg || error.message || '购买失败'
    ElMessage.error(errorMessage.includes('余额不足') ? '余额不足，请先充值' : errorMessage)
  } finally {
    loading.close()
  }
}

// 查看笔记详情
const handleViewNote = (note: NoteVO) => {
  currentNote.value = note
  detailNoteDialogVisible.value = true
}

// 打开编辑弹窗
const handleEditNote = (note: NoteVO) => {
  editNoteForm.id = note.id
  editNoteForm.title = note.title
  editNoteForm.content = note.content
  editNoteForm.price = note.price
  editNoteForm.img = note.img
  editNoteDialogVisible.value = true
  detailNoteDialogVisible.value = false
}

// 更新笔记处理
const updateNoteHandler = async () => {
  try {
    await updateNote({
      id: editNoteForm.id,
      title: editNoteForm.title,
      content: editNoteForm.content,
      price: editNoteForm.price,
      img: editNoteForm.img
    })

    // 更新本地数据
    const index = searchResults.value.notes.findIndex(n => n.id === editNoteForm.id)
    if (index > -1) {
      searchResults.value.notes[index] = {
        ...searchResults.value.notes[index],
        ...editNoteForm
      }
    }

    ElMessage.success('笔记更新成功')
    editNoteDialogVisible.value = false
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

// 处理图片上传
const handleEditNoteImageUpload = async (params: any) => {
  const loading = ElLoading.service({fullscreen: false})
  try {
    const {file} = params
    const response = await uploadUserImage(file)
    editNoteForm.img = response.data.data
    ElMessage.success('图片上传成功')
  } catch (error) {
    ElMessage.error('图片上传失败')
  } finally {
    loading.close()
  }
}

// 处理商品点击
const handleProductClick = (productId: string) => {
  router.push(`/product/${productId}`)
}

// 购物车相关处理函数
const handleCartAdd = async (productId: string) => {
  try {
    const { addToCart } = await import('../../api/cart')
    await addToCart(productId, 1)
    // 更新本地购物车状态
    if (cartItems.value[productId]) {
      cartItems.value[productId].quantity++
    } else {
      cartItems.value[productId] = { productId, quantity: 1 }
    }
    ElMessage.success('已添加到购物车')
  } catch (error) {
    ElMessage.error('添加到购物车失败')
  }
}

const handleCartSubtract = async (productId: string) => {
  try {
    const { updateCartItemQuantity } = await import('../../api/cart')
    const currentQuantity = cartItems.value[productId]?.quantity || 0
    if (currentQuantity > 1) {
      await updateCartItemQuantity(cartItems.value[productId].cartItemId, currentQuantity - 1)
      cartItems.value[productId].quantity--
    } else {
      const { deleteCartItem } = await import('../../api/cart')
      await deleteCartItem(cartItems.value[productId].cartItemId)
      delete cartItems.value[productId]
    }
    ElMessage.success('购物车已更新')
  } catch (error) {
    ElMessage.error('更新购物车失败')
  }
}

const handleCartUpdated = () => {
  // 刷新购物车数据
  fetchSearchResults()
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
      await cancelCollectBookList({bookListId: bookList.id})
      bookList.favouriteCount--
      favouriteBookListIds.value.delete(bookList.id)
      ElMessage.success('取消收藏成功')
    } else {
      await collectBookList({bookListId: bookList.id})
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
const handleView = async (bookList: BookListVO) => {
  detailLoading.value = true
  try {
    const res = await getAllBookLists(0, 1000)
    const fullBookList = res.data.data.content.find(list => list.id === bookList.id)
    if (fullBookList) {
      currentBookList.value = fullBookList
      detailDialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('获取书单详情失败')
  } finally {
    detailLoading.value = false
  }
}

watch(
  () => route.query.keyword,
  async (newKeyword) => {
    if (newKeyword) {
      await fetchSearchResults()
    }
  }
)

onMounted(async () => {
  // 获取当前用户ID
  const userId = sessionStorage.getItem('userId')
  if (userId) {
    currentUserId.value = Number(userId)
  }
  
  // 设置管理员状态
  const role = sessionStorage.getItem('role')
  isAdmin.value = role === 'admin'
  
  await fetchSearchResults()
})
</script>

<template>
  <div class="search-result-page">
    <Header/>
    <div class="search-result-container" v-loading="loading">
      <h2 class="search-title">搜索结果: {{ route.query.keyword }}</h2>

      <!-- 导航栏 -->
      <div class="search-tabs">
        <div
            v-for="tab in [
            { key: 'products', label: '商品', count: searchResults.products.length },
            { key: 'accounts', label: '用户', count: searchResults.accounts.length },
            { key: 'bookLists', label: '书单', count: searchResults.bookLists.length },
            { key: 'notes', label: '读书笔记', count: searchResults.notes.length }
          ]"
            :key="tab.key"
            :class="['tab-item', { active: activeTab === tab.key }]"
            @click="activeTab = tab.key"
        >
          {{ tab.label }}
          <span class="count">({{ tab.count }})</span>
        </div>
      </div>

      <!-- 读书笔记内容 -->
      <div v-show="activeTab === 'notes'" class="result-content">
        <div class="notes-grid">
          <ReadingNote
              v-for="note in searchResults.notes"
              :key="note.id"
              :note="note"
              :is-liked="likedNoteIds.has(note.id)"
              :is-creator="currentUserId === note.creatorId"
              :is-paid="paidNoteIds.has(note.id)"
              @like="handleLikeNote"
              @unlike="handleUnlikeNote"
              @delete="handleDeleteNote"
              @purchase="handlePurchaseNote"
              @view="handleViewNote"
          />
        </div>
      </div>
      <!-- 商品结果 -->
      <div v-show="activeTab === 'products'" class="result-content">
        <div class="product-grid">
          <ProductCard
            v-for="product in searchResults.products"
            :key="product.id"
            :product="product"
            :stockpile="stockpiles[product.id] || { amount: 0, frozen: 0 }"
            :is-admin="isAdmin"
            :cart-items="cartItems"
            :has-advertisement="false"
            @cart-add="handleCartAdd"
            @cart-subtract="handleCartSubtract"
            @cart-updated="handleCartUpdated"
          />
        </div>
      </div>

      <!-- 用户结果 -->
      <div v-show="activeTab === 'accounts'" class="result-content">
        <div class="user-grid">
          <VUserCard
              v-for="account in searchResults.accounts"
              :key="account.id"
              :user="{
          id: account.id,
          username: account.username,
          avatar: account.avatar,
          isVerified: account.verifiedName !== null,
          bio: '',
          verifiedName: account.verifiedName || ''
        }"
          />
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

      <!-- 笔记详情弹窗 -->
      <el-dialog
          v-model="detailNoteDialogVisible"
          title="笔记详情"
          width="600px"
      >
        <div v-if="currentNote" class="note-detail">
          <div class="detail-header">
            <h2>{{ currentNote.title }}</h2>
            <div class="detail-price" :class="{ 'paid': paidNoteIds.has(currentNote.id) }">
              <template v-if="currentNote.price > 0">
                {{ currentNote.price }} 🍅
                <span v-if="paidNoteIds.has(currentNote.id)" class="paid-badge">已购买</span>
              </template>
              <span v-else class="free">免费</span>
            </div>
          </div>

          <el-image
              v-if="currentNote.img"
              :src="currentNote.img"
              class="note-image"
              style="max-width: 100%; margin: 10px 0;"
          />

          <!-- 修改内容展示部分 -->
          <div class="note-content-container">
            <div
                class="note-content"
                :class="{ 'limited-content': currentNote.price > 0 && !paidNoteIds.has(currentNote.id) }"
                style="white-space: pre-wrap;"
            >
              {{ getDisplayContent(currentNote.content, paidNoteIds.has(currentNote.id)) }}
            </div>

            <!-- 未购买提示 -->
            <div
                v-if="currentNote.price > 0 && !paidNoteIds.has(currentNote.id)"
                class="purchase-tip"
            >
              <el-alert
                  title="预览内容已结束，购买后可查看完整笔记"
                  type="warning"
                  :closable="false"
                  show-icon
              />
              <el-button
                  type="primary"
                  class="purchase-button"
                  @click="handlePurchaseNote(currentNote)"
              >
                立即解锁（{{ currentNote.price }} 🍅）
              </el-button>
            </div>
          </div>

          <div class="actions" v-if="currentUserId === currentNote.creatorId" style="margin-top: 20px;">
            <el-button type="primary" @click="handleEditNote(currentNote)">编辑笔记</el-button>
            <el-button type="danger" @click="handleDeleteNote(currentNote.id)">删除笔记</el-button>
          </div>
        </div>
      </el-dialog>

      <!-- 编辑笔记弹窗 -->
      <el-dialog
          v-model="editNoteDialogVisible"
          title="编辑笔记"
          width="600px"
      >
        <el-form :model="editNoteForm" label-width="80px">
          <el-form-item label="标题" required>
            <el-input v-model="editNoteForm.title"/>
          </el-form-item>
          <el-form-item label="内容" required>
            <el-input
                v-model="editNoteForm.content"
                type="textarea"
                :rows="4"
                resize="none"
            />
          </el-form-item>
          <el-form-item label="价格">
            <el-input-number
                v-model="editNoteForm.price"
                :min="0"
                :precision="0"
            />
          </el-form-item>
          <el-form-item label="封面图">
            <el-upload
                :auto-upload="true"
                :http-request="handleEditNoteImageUpload"
                :show-file-list="false"
            >
              <template #trigger>
                <el-button type="primary">上传新图片</el-button>
              </template>
              <img
                  v-if="editNoteForm.img"
                  :src="editNoteForm.img"
                  class="preview-image"
                  style="max-width: 200px; margin-top: 10px;"
              />
            </el-upload>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editNoteDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="updateNoteHandler">保存修改</el-button>
        </template>
      </el-dialog>

      <!-- 购买确认弹窗 -->
      <el-dialog
          v-model="showPurchaseDialog"
          title="确认购买"
          width="500px"
          class="purchase-confirm-dialog"
      >
        <div v-if="selectedNote" class="confirm-purchase">
          <img
              :src="selectedNote.img || '/default-note-cover.png'"
              class="note-cover"
              alt="笔记封面"
          />
          <div class="content">
            <h3>是否确认购买《{{ selectedNote.title }}》？</h3>
            <p class="price">价格：{{ selectedNote.price }} 🍅</p>
          </div>
        </div>
        <template #footer>
          <el-button @click="showPurchaseDialog = false">取消</el-button>
          <el-button type="primary" @click="confirmPurchase">确认购买</el-button>
        </template>
      </el-dialog>

      <!-- 书单详情对话框 -->
      <el-dialog
          v-model="detailDialogVisible"
          title="书单详情"
          width="800px"
      >
        <div v-loading="detailLoading" class="booklist-detail">
          <template v-if="currentBookList">
            <div class="booklist-header">
              <h2>{{ currentBookList.title }}</h2>
              <div class="creator-info">
                <img :src="currentBookList.creatorAvatar" :alt="currentBookList.creatorName" class="creator-avatar">
                <span>{{ currentBookList.creatorName }}</span>
                <span class="creation-date">{{ new Date(currentBookList.creationDate).toLocaleDateString() }}</span>
              </div>
            </div>
            <p class="description">{{ currentBookList.description }}</p>

            <div class="products-list">
              <div
                  v-for="product in currentBookList.products"
                  :key="product.id"
                  class="product-item"
                  @click="handleProductClick(product.id)"
              >
                <img :src="product.cover" :alt="product.title" class="product-cover">
                <div class="product-info">
                  <h4>{{ product.title }}</h4>
                  <p class="price">¥{{ product.price }}</p>
                </div>
              </div>
            </div>

            <div class="booklist-footer">
              <span class="favourite-count">
                <el-icon><Star/></el-icon>
                {{ currentBookList.favouriteCount }} 收藏
              </span>
              <span class="product-count">
                {{ currentBookList.products.length }} 个商品
              </span>
            </div>
          </template>
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
  padding: 80px 24px 24px 24px;
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
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  padding: 20px 0;
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
  grid-template-columns: repeat(3, minmax(280px, 1fr));
  gap: 24px;
  padding: 16px;
}

@media (max-width: 1200px) {
  .user-grid {
    grid-template-columns: repeat(2, minmax(280px, 1fr));
  }
}

@media (max-width: 768px) {
  .user-grid {
    grid-template-columns: repeat(1, minmax(280px, 1fr));
  }
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

.booklist-header {
  margin-bottom: 20px;
}

.creator-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  color: #606266;
}

.creator-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.creation-date {
  color: #909399;
  font-size: 14px;
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
  cursor: pointer;
  transition: all 0.3s;
}

.product-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
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

.booklist-footer {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 14px;
}

.favourite-count {
  display: flex;
  align-items: center;
  gap: 4px;
}

.product-count {
  color: #606266;
}

.notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
}

.note-detail {
  padding: 20px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.detail-price {
  font-size: 18px;
  color: #e6a23c;
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-price.paid {
  color: #67c23a;
}

.paid-badge {
  background: #f0f9eb;
  color: #67c23a;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 14px;
}

.note-image {
  border-radius: 8px;
  margin-bottom: 16px;
}

.confirm-purchase {
  text-align: center;
  padding: 20px;
}

.note-cover {
  max-width: 200px;
  max-height: 150px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.purchase-confirm-dialog .el-dialog__body {
  padding: 20px;
}

.content h3 {
  margin: 0 0 12px 0;
  color: #303133;
}

.content .price {
  color: #e6a23c;
  font-weight: bold;
  font-size: 16px;
}

.preview-image {
  max-width: 200px;
  border-radius: 4px;
  margin-top: 10px;
}

.notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
  padding: 20px;
}

.note-content-container {
  position: relative;
}

.limited-content {
  position: relative;
  max-height: 200px;
  overflow: hidden;
}

.limited-content::after {
  content: "";
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: linear-gradient(transparent, white);
}

.purchase-tip {
  margin-top: 20px;
  text-align: center;
  border-top: 1px solid #eee;
  padding-top: 20px;
}

.purchase-button {
  margin-top: 15px;
  width: 100%;
}
</style>
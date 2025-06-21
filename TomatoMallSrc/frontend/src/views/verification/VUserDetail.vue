<template>
  <div class="user-detail">
    <el-skeleton v-if="loading" :rows="6" animated/>

    <template v-else>
      <!-- 用户信息 -->
      <div class="profile">
        <div class="profile-header">
          <el-image
              :src="user.avatar"
              class="avatar"
              fit="cover"
          >
            <template #error>
              <div class="avatar-error">
                <el-icon :size="40">
                  <User/>
                </el-icon>
              </div>
            </template>
          </el-image>
          <h1>{{ user.username }}</h1>
          <UserBadge :is-verified="user.isVerified" :verified-name="user.verifiedName"/>
          <div class="profile-actions">
            <el-button
                v-if="currentId != user.id"
                :type="isFollowing ? 'info' : 'primary'"
                @click="handleFollow"
                size="medium"
            >
              {{ isFollowing ? '已关注' : '+ 关注' }}
            </el-button>
          </div>
        </div>
      </div>

      <!-- 内容切换 -->
      <div class="tabs-container">
        <el-tabs v-model="activeTab" class="custom-tabs">
          <!-- 书单列表 -->
          <el-tab-pane label="创建的书单" name="booklists">
            <template #label>
              <div class="tab-label">
                <el-icon><Collection /></el-icon>
                <span>创建的书单</span>
              </div>
            </template>
            <div v-if="booklists.length === 0" class="empty-tip">
              <el-empty description="该用户暂未创建书单"/>
            </div>
            <div v-else class="horizontal-list">
              <div v-for="list in booklists" :key="list.id" class="card-wrapper booklist-wrapper">
                <BookListItem
                    :book-list="list"
                    :is-favourite="list.isFavourite"
                    :is-creator="false"
                    @collect="handleCollectBookList"
                    @view="handleViewBookList"
                />
              </div>
            </div>
          </el-tab-pane>

          <!-- 笔记列表 -->
          <el-tab-pane label="读书笔记" name="notes">
            <template #label>
              <div class="tab-label">
                <el-icon><Notebook /></el-icon>
                <span>读书笔记</span>
              </div>
            </template>
            <div v-if="notes.length === 0" class="empty-tip">
              <el-empty description="该用户暂未发布笔记"/>
            </div>
            <div v-else class="horizontal-list">
              <div v-for="note in notes" :key="note.id" class="card-wrapper note-wrapper">
                <ReadingNote
                    :note="note"
                    :is-liked="likedNoteIds.has(note.id)"
                    :is-creator="false"
                    :is-paid="paidNoteIds.has(note.id)"
                    @like="handleLikeNote"
                    @unlike="handleUnlikeNote"
                    @purchase="handlePurchaseNote"
                    @view="handleViewNote"
                />
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 书单详情弹窗 -->
      <el-dialog
          v-model="bookListDetailVisible"
          title="书单详情"
          width="800px"
      >
        <div :loading="detailLoading" class="booklist-detail">
          <template v-if="currentBookList">
            <div class="booklist-header">
              <h2>{{ currentBookList.title }}</h2>
              <div class="creator-info">
                <img :src="currentBookList.creatorAvatar" class="creator-avatar">
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
                <img :src="product.cover" class="product-cover">
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

      <!-- 笔记详情弹窗 -->
      <el-dialog
          v-model="noteDetailVisible"
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
          />

          <!-- 修改内容展示结构 -->
          <div class="note-content-container">
            <div
                class="note-content"
                :class="{ 'limited-content': currentNote.price > 0 && !paidNoteIds.has(currentNote.id) }"
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
        </div>
      </el-dialog>

      <!-- 购买确认弹窗 -->
      <el-dialog
          v-model="purchaseVisible"
          title="确认购买"
          width="500px"
      >
        <div v-if="selectedNote" class="confirm-purchase">
          <!-- 添加图片容器 -->
          <div class="cover-container">
            <img :src="selectedNote.img" class="note-cover">
          </div>
          <h3>{{ selectedNote.title }}</h3>
          <p class="price">价格：{{ selectedNote.price }} 🍅</p>
          <el-button type="primary" @click="confirmPurchase">确认购买</el-button>
        </div>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import {ref, computed, onMounted, reactive} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage, ElLoading} from 'element-plus'
import {User, Star, Collection, Notebook} from '@element-plus/icons-vue'
import {
  getUserBookLists,
  collectBookList,
  cancelCollectBookList,
  getAllBookLists
} from '../../api/booklist'
import {
  getUserNotes,
  payNote,
  likeNote,
  unlikeNote,
  getNoteLikeStatus,
  getNotePayStatus
} from '../../api/note'
import UserBadge from '../../components/UserBadge.vue'
import ReadingNote from '../../components/ReadingNote.vue'
import BookListItem from '../../components/BookListItem.vue'
import { checkIsFollowed, followUser, unfollowUser } from '../../api/user'

const route = useRoute()
const userId = computed(() => Number(route.params.userId) || 0)
const router = useRouter()
const isFollowing = ref(false)
const currentId = Number(sessionStorage.getItem('userId'))
const activeTab = ref('booklists')
const loading = ref(true)

const getFollowStatus = async () => {
  try {
    const res = await checkIsFollowed(user.id)
    isFollowing.value = (res as any).data?.data || false
  } catch (error) {
    console.error('获取关注状态失败:', error)
  }
}

const handleFollow = async () => {
  try {
    if (isFollowing.value) {
      await unfollowUser(user.id)
    } else {
      await followUser(user.id)
    }
    isFollowing.value = !isFollowing.value
  } catch (error) {
    console.error('操作失败:', error)
  }
}
// 用户信息
const user = reactive({
  id: userId,
  avatar: computed(() => route.query.avatar as string || ''),
  username: computed(() => route.query.username as string || ''),
  isVerified: computed(() => route.query.isVerified === 'true'),
  verifiedName: computed(() => route.query.verifiedName as string || '')
})

// 书单相关
const booklists = ref<any[]>([])
const bookListDetailVisible = ref(false)
const currentBookList = ref<any>(null)
const detailLoading = ref(false)

// 笔记相关
const notes = ref<any[]>([])
const likedNoteIds = ref(new Set<number>())
const paidNoteIds = ref(new Set<number>())
const noteDetailVisible = ref(false)
const currentNote = ref<any>(null)
const purchaseVisible = ref(false)
const selectedNote = ref<any>(null)

// 初始化加载
onMounted(async () => {
  loading.value = true
  try {
    // 加载书单
    const listRes = await getUserBookLists(userId.value)
    booklists.value = (listRes as any).data?.data?.content || []

    // 加载笔记
    const noteRes = await getUserNotes(userId.value)
    notes.value = (noteRes as any).data?.data || []

    // 初始化笔记状态
    for (const note of notes.value) {
      const [likeRes, payRes] = await Promise.all([
        getNoteLikeStatus(note.id),
        getNotePayStatus(note.id)
      ])
      if ((likeRes as any).data?.data) likedNoteIds.value.add(note.id)
      if ((payRes as any).data?.data) paidNoteIds.value.add(note.id)
    }
    await getFollowStatus()
  } catch (error) {
    ElMessage.error('数据加载失败')
  } finally {
    loading.value = false
  }
})

// 书单操作
const handleCollectBookList = async (bookList: any) => {
  try {
    if (bookList.isFavourite) {
      await cancelCollectBookList({bookListId: bookList.id})
      bookList.favouriteCount--
    } else {
      await collectBookList({bookListId: bookList.id})
      bookList.favouriteCount++
    }
    bookList.isFavourite = !bookList.isFavourite
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleViewBookList = async (bookList: any) => {
  detailLoading.value = true
  try {
    const res = await getAllBookLists(0, 1000)
    const fullBookList = (res as any).data?.data?.content?.find((list: any) => list.id === bookList.id)

    if (fullBookList) {
      currentBookList.value = {
        ...fullBookList,
        creatorAvatar: fullBookList.creatorAvatar || user.avatar,
        creatorName: fullBookList.creatorName || user.username
      }
      bookListDetailVisible.value = true
    }
  } catch (error) {
    ElMessage.error('获取书单详情失败')
  } finally {
    detailLoading.value = false
  }
}

// 商品点击处理
const handleProductClick = (productId: string) => {
  router.push(`/product/${productId}`)
}

// 笔记操作
const handleLikeNote = async (note: any) => {
  try {
    await likeNote(note.id)
    likedNoteIds.value.add(note.id)
    note.likeCnt++
  } catch (error) {
    ElMessage.error('点赞失败')
  }
}

const handleUnlikeNote = async (note: any) => {
  try {
    await unlikeNote(note.id)
    likedNoteIds.value.delete(note.id)
    note.likeCnt--
  } catch (error) {
    ElMessage.error('取消点赞失败')
  }
}

const handleViewNote = (note: any) => {
  currentNote.value = note
  noteDetailVisible.value = true
}

const handlePurchaseNote = (note: any) => {
  selectedNote.value = note
  purchaseVisible.value = true
}

const confirmPurchase = async () => {
  if (!selectedNote.value) return
  try {
    await payNote(selectedNote.value.id)
    paidNoteIds.value.add(selectedNote.value.id)
    ElMessage.success('购买成功')
    purchaseVisible.value = false
  } catch (error: any) {
    ElMessage.error(error.response?.data?.msg || '购买失败')
  }
}

const getDisplayContent = (content: string, isPaid: boolean) => {
  if (isPaid || !content) return content
  const showLength = Math.ceil(content.length * 0.35)
  return content.slice(0, showLength) + '...'
}
</script>

<style scoped>
.user-detail {
  min-height: 100vh;
  background: #ffffff;
  padding: 20px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 用户信息区域 */
.profile {
  background: #ffffff;
  border-radius: 20px;
  padding: 40px;
  margin-bottom: 30px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.profile:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.12);
}

.profile-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 20px;
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 4px solid #fff;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  transition: transform 0.3s ease;
}

.avatar:hover {
  transform: scale(1.05);
}

.avatar-error {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(45deg, #f093fb 0%, #f5576c 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  border: 4px solid #fff;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.profile-header h1 {
  font-size: 2.5rem;
  font-weight: 700;
  color: #2c3e50;
  margin: 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.profile-actions {
  margin-top: 10px;
}

.profile-actions .el-button {
  border-radius: 25px;
  padding: 12px 30px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.profile-actions .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

/* 标签页容器 */
.tabs-container {
  background: #ffffff;
  border-radius: 20px;
  padding: 30px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
}

.custom-tabs {
  --el-tabs-header-height: 60px;
}

.custom-tabs :deep(.el-tabs__header) {
  margin-bottom: 30px;
  border-bottom: 2px solid #f0f0f0;
}

.custom-tabs :deep(.el-tabs__nav-wrap) {
  padding: 0 20px;
}

.custom-tabs :deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 600;
  color: #666;
  transition: all 0.3s ease;
  border-radius: 10px 10px 0 0;
  margin-right: 10px;
}

.custom-tabs :deep(.el-tabs__item:hover) {
  color: #409eff;
  background: rgba(64, 158, 255, 0.1);
}

.custom-tabs :deep(.el-tabs__item.is-active) {
  color: #409eff;
  background: rgba(64, 158, 255, 0.1);
  border-bottom: 3px solid #409eff;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tab-label .el-icon {
  font-size: 18px;
}

/* 空状态提示 */
.empty-tip {
  text-align: center;
  padding: 60px 20px;
}

.empty-tip :deep(.el-empty__description) {
  color: #999;
  font-size: 16px;
}

/* 水平列表布局 */
.horizontal-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 25px;
  padding: 20px 0;
}

.card-wrapper {
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.card-wrapper:hover {
  transform: translateY(-8px);
}

.booklist-wrapper :deep(.booklist-card),
.note-wrapper :deep(.reading-note) {
  border-radius: 15px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.booklist-wrapper :deep(.booklist-card:hover),
.note-wrapper :deep(.reading-note:hover) {
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.15);
  transform: translateY(-5px);
}

/* 弹窗样式优化 */
:deep(.el-dialog) {
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.15);
}

:deep(.el-dialog__header) {
  background: #409eff;
  color: white;
  padding: 25px 30px;
  margin: 0;
}

:deep(.el-dialog__title) {
  color: white;
  font-weight: 600;
  font-size: 18px;
}

:deep(.el-dialog__headerbtn) {
  top: 25px;
  right: 30px;
}

:deep(.el-dialog__headerbtn .el-dialog__close) {
  color: #ff4757;
  font-size: 20px;
  font-weight: bold;
  transition: all 0.3s ease;
}

:deep(.el-dialog__headerbtn .el-dialog__close:hover) {
  color: #ff3742;
  transform: scale(1.1);
}

:deep(.el-dialog__body) {
  padding: 30px;
}

/* 书单详情弹窗 */
.booklist-detail {
  max-height: 70vh;
  overflow-y: auto;
}

.booklist-header {
  margin-bottom: 25px;
  padding-bottom: 20px;
  border-bottom: 2px solid #f0f0f0;
}

.booklist-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: #2c3e50;
  margin: 0 0 15px 0;
}

.creator-info {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #666;
  font-size: 14px;
}

.creator-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.creation-date {
  color: #999;
  font-size: 12px;
}

.description {
  color: #555;
  line-height: 1.6;
  margin-bottom: 25px;
  font-size: 15px;
}

.products-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
  margin-bottom: 25px;
}

.product-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px;
  border-radius: 12px;
  background: #f8f9fa;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #e9ecef;
}

.product-item:hover {
  background: #e9ecef;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.product-cover {
  width: 50px;
  height: 70px;
  border-radius: 8px;
  object-fit: cover;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.1);
}

.product-info h4 {
  margin: 0 0 5px 0;
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
}

.price {
  color: #e74c3c;
  font-weight: 600;
  font-size: 14px;
  margin: 0;
}

.booklist-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20px;
  border-top: 2px solid #f0f0f0;
  color: #666;
  font-size: 14px;
}

.favourite-count,
.product-count {
  display: flex;
  align-items: center;
  gap: 5px;
}

/* 笔记详情弹窗 */
.note-detail {
  max-height: 70vh;
  overflow-y: auto;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 25px;
  padding-bottom: 20px;
  border-bottom: 2px solid #f0f0f0;
}

.detail-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: #2c3e50;
  margin: 0;
  flex: 1;
}

.detail-price {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #e74c3c;
}

.detail-price.free {
  color: #27ae60;
}

.paid-badge {
  background: #27ae60;
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.note-image {
  width: 100%;
  max-height: 300px;
  border-radius: 12px;
  margin-bottom: 25px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.note-content-container {
  position: relative;
}

.note-content {
  line-height: 1.8;
  color: #2c3e50;
  font-size: 15px;
  white-space: pre-wrap;
}

.note-content.limited-content {
  position: relative;
  max-height: 200px;
  overflow: hidden;
}

.note-content.limited-content::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: linear-gradient(transparent, white);
  pointer-events: none;
}

.purchase-tip {
  margin-top: 25px;
  text-align: center;
}

.purchase-button {
  margin-top: 15px;
  border-radius: 25px;
  padding: 12px 30px;
  font-weight: 600;
  background: #409eff;
  border: none;
  transition: all 0.3s ease;
}

.purchase-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(64, 158, 255, 0.3);
}

/* 购买确认弹窗 */
.confirm-purchase {
  text-align: center;
  padding: 20px 0;
}

.cover-container {
  margin-bottom: 20px;
}

.note-cover {
  width: 200px;
  height: 280px;
  border-radius: 12px;
  object-fit: cover;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.confirm-purchase h3 {
  font-size: 20px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 15px 0;
}

.confirm-purchase .price {
  font-size: 18px;
  font-weight: 600;
  color: #e74c3c;
  margin-bottom: 25px;
}

.confirm-purchase .el-button {
  border-radius: 25px;
  padding: 12px 30px;
  font-weight: 600;
  background: #409eff;
  border: none;
  transition: all 0.3s ease;
}

.confirm-purchase .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(64, 158, 255, 0.3);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-detail {
    padding: 15px;
  }
  
  .profile {
    padding: 30px 20px;
  }
  
  .profile-header h1 {
    font-size: 2rem;
  }
  
  .avatar,
  .avatar-error {
    width: 100px;
    height: 100px;
  }
  
  .tabs-container {
    padding: 20px;
  }
  
  .horizontal-list {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  
  .products-list {
    grid-template-columns: 1fr;
  }
  
  .detail-header {
    flex-direction: column;
    gap: 15px;
  }
  
  .booklist-footer {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
}

@media (max-width: 480px) {
  .profile {
    padding: 25px 15px;
  }
  
  .profile-header h1 {
    font-size: 1.8rem;
  }
  
  .tabs-container {
    padding: 15px;
  }
  
  .custom-tabs :deep(.el-tabs__item) {
    font-size: 14px;
    padding: 0 15px;
  }
}

/* 加载动画优化 */
:deep(.el-skeleton) {
  background: #ffffff;
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
}

/* 滚动条美化 */
.note-detail::-webkit-scrollbar,
.booklist-detail::-webkit-scrollbar {
  width: 6px;
}

.note-detail::-webkit-scrollbar-track,
.booklist-detail::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.note-detail::-webkit-scrollbar-thumb,
.booklist-detail::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.note-detail::-webkit-scrollbar-thumb:hover,
.booklist-detail::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>
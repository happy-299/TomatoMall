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
  padding: 80px 20px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

/* 用户资料部分 */
.profile {
  padding: 0 0 30px;
}

.profile-header {
  text-align: center;
  padding: 30px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  position: relative;
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  margin-bottom: 15px;
  border: 4px solid white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.avatar-error {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background-color: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.profile h1 {
  margin: 15px 0 10px;
  font-size: 28px;
  color: #303133;
}

.profile-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
}

.profile-actions .el-button {
  padding: 10px 30px;
  border-radius: 24px;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 标签页样式 */
.tabs-container {
  margin-bottom: 30px;
}

.custom-tabs {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.custom-tabs :deep(.el-tabs__header) {
  margin-bottom: 30px;
}

.custom-tabs :deep(.el-tabs__nav-wrap) {
  justify-content: center;
}

.custom-tabs :deep(.el-tabs__nav) {
  border-radius: 30px;
  background-color: #f8f9fa;
  padding: 5px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.custom-tabs :deep(.el-tabs__item) {
  font-size: 16px;
  padding: 0 24px;
  height: 50px;
  line-height: 50px;
  transition: all 0.3s;
  border-radius: 25px;
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

/* 书单和笔记列表 */
.horizontal-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  width: 100%;
  padding: 12px 0;
}

.card-wrapper {
  height: 520px;
  width: 100%;
  margin: 0;
  position: relative;
}

/* 修复BookListItem样式 */
.booklist-wrapper :deep(.booklist-card) {
  margin: 0;
  height: 100%;
  width: 100%;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
}

.booklist-wrapper :deep(.booklist-cover) {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.booklist-wrapper :deep(.cover-image) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 修复ReadingNote样式 */
.note-wrapper :deep(.note-card) {
  margin: 0;
  height: 100%;
  width: 100%;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.note-wrapper :deep(.note-image) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.note-wrapper :deep(.note-info) {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 32px 24px 24px;
  background: linear-gradient(0deg, rgba(0,0,0,0.85) 60%, rgba(0,0,0,0.2) 100%, transparent 100%) !important;
  z-index: 2;
}

/* Fix empty state */
.empty-tip {
  padding: 40px 0;
  text-align: center;
  grid-column: 1 / -1;
}

/* 书单详情弹窗样式 */
.booklist-detail {
  padding: 20px;
  max-height: 70vh;
  overflow-y: auto;
}

.booklist-header h2 {
  margin: 0;
  font-size: 22px;
  color: #303133;
}

.creator-info {
  display: flex;
  align-items: center;
  margin: 12px 0;
  font-size: 14px;
}

.creator-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  margin-right: 8px;
}

.creation-date {
  color: #909399;
  margin-left: 12px;
  font-size: 12px;
}

.description {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 16px 0;
}

.products-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.product-item {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
}

.product-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.product-cover {
  width: 100%;
  height: 180px;
  object-fit: cover;
  border-radius: 4px;
  margin-bottom: 8px;
}

.product-info {
  flex: 1;
}

.product-info h4 {
  margin: 0;
  font-size: 14px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.price {
  color: #f56c6c;
  font-weight: bold;
  margin-top: 8px;
  font-size: 16px;
}

.booklist-footer {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  color: #606266;
  font-size: 14px;
}

/* 笔记详情样式 */
.note-detail {
  padding: 10px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.detail-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.detail-price {
  font-size: 16px;
  color: #e6a23c;
  font-weight: bold;
}

.paid {
  color: #67c23a;
}

.paid-badge {
  font-size: 12px;
  background-color: #67c23a;
  color: white;
  padding: 2px 6px;
  border-radius: 10px;
  margin-left: 5px;
}

.free {
  color: #909399;
  font-weight: normal;
}

.note-image {
  width: 100%;
  max-height: 300px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.note-content-container {
  position: relative;
  margin: 16px 0;
}

.note-content {
  white-space: pre-wrap;
  line-height: 1.6;
  font-size: 14px;
  color: #606266;
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

/* 购买确认弹窗样式 */
.confirm-purchase {
  text-align: center;
}

.cover-container {
  width: 100%;
  height: 200px;
  border-radius: 8px;
  overflow: hidden;
  margin: 0 auto 20px;
  background: #f5f7fa;
}

.note-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.confirm-purchase h3 {
  margin: 0 0 12px;
  font-size: 18px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.confirm-purchase .price {
  color: #e6a23c;
  font-size: 20px;
  margin: 0 0 20px;
  font-weight: bold;
}

.confirm-purchase .el-button {
  width: 80%;
  margin-top: 10px;
}

/* 响应式布局调整 */
@media (max-width: 1200px) {
  .horizontal-list {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .user-detail {
    padding: 60px 15px 15px;
  }
  
  .profile-header {
    padding: 20px;
  }
  
  .profile h1 {
    font-size: 22px;
  }
  
  .avatar {
    width: 100px;
    height: 100px;
  }
  
  .horizontal-list {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .custom-tabs :deep(.el-tabs__item) {
    padding: 0 16px;
    font-size: 14px;
    height: 40px;
    line-height: 40px;
  }
}
</style>
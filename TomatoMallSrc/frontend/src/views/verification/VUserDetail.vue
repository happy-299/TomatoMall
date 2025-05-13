<template>
  <div class="user-detail">
    <el-skeleton v-if="loading" :rows="6" animated />

    <template v-else>
      <!-- 用户信息 -->
      <div class="profile">
        <el-image
            :src="user.avatar"
            class="avatar"
            fit="cover"
        >
          <template #error>
            <div class="avatar-error">
              <el-icon :size="40"><User /></el-icon>
            </div>
          </template>
        </el-image>
        <h1>{{ user.username }}</h1>
        <UserBadge :is-verified="user.isVerified" />
      </div>

      <!-- 内容切换 -->
      <el-tabs v-model="activeTab">
        <!-- 书单列表（使用新组件） -->
        <el-tab-pane label="创建的书单" name="booklists">
          <div v-if="booklists.length === 0" class="empty-tip">
            <el-empty description="该用户暂未创建书单" />
          </div>
          <div class="horizontal-list">
          <BookListItem
              v-for="list in booklists"
              :key="list.id"
              :book-list="list"
              :is-favourite="list.isFavourite"
              :is-creator="list.creatorId === currentUserId"
              @collect="handleCollectBookList"
              @delete="handleDeleteBookList"
              @view="handleViewBookList"
          />
          </div>
        </el-tab-pane>

        <!-- 笔记列表（使用新组件） -->
        <el-tab-pane label="读书笔记" name="notes">
          <div v-if="notes.length === 0" class="empty-tip">
            <el-empty description="该用户暂未发布笔记" />
          </div >
          <div class="horizontal-list">
          <ReadingNote
              v-for="note in notes"
              :key="note.id"
              :note="note"
              :is-liked="note.isLiked"
              :is-creator="note.creatorId === currentUserId"
              :is-paid="note.isPaid"
              @like="handleLike"
              @unlike="handleUnlike"
              @delete="handleDeleteNote"
              @purchase="handlePurchaseNote"
              @view="handleViewNote"
          />
          </div >
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import {
  getUserBookLists,
  collectBookList,
  cancelCollectBookList,
  deleteBookList
} from '../../api/booklist.ts'
import {
  getUserNotes,
  getNoteLikeStatus,
  getNotePayStatus,
  deleteNote,
  payNote
} from '../../api/note.ts'
import { getUserInfo } from '../../api/user.ts'
import UserBadge from '../../components/UserBadge.vue'
import ReadingNote from '../../components/ReadingNote.vue'
import BookListItem from '../../components/BookListItem.vue'
import axios from "axios";

const route = useRoute()
const router = useRouter()
// 当前登录用户ID
const currentUserId = computed(() => {
  return Number(sessionStorage.getItem('userId')) || 0
})

// 安全解析用户ID
const userId = computed(() => {
  const id = Number(route.params.userId)
  return isNaN(id) ? 0 : id
})

const user = ref<any>({})
const booklists = ref<any[]>([])
const notes = ref<any[]>([])
const activeTab = ref('booklists')
const loading = ref(true)

// 格式化时间显示
const formatTime = (timeStr: string) => {
  return new Date(timeStr).toLocaleDateString()
}

//书单
// 获取书单收藏状态
const fetchBookListStatus = async (bookListId: number) => {
  try {
    const res = await axios.get(`/api/booklist/favourite-status/${bookListId}`)
    return res.data
  } catch (error) {
    console.error('获取收藏状态失败:', error)
    return false
  }
}

// 书单事件处理
const handleCollectBookList = async (bookList: any) => {
  try {
    if (bookList.isFavourite) {
      await cancelCollectBookList({ bookListId: bookList.id })
      ElMessage.success('已取消收藏')
    } else {
      await collectBookList({ bookListId: bookList.id })
      ElMessage.success('收藏成功')
    }
    bookList.isFavourite = !bookList.isFavourite
    bookList.favouriteCount += bookList.isFavourite ? 1 : -1
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const handleDeleteBookList = async (id: number) => {
  try {
    await deleteBookList(id)
    booklists.value = booklists.value.filter(list => list.id !== id)
    ElMessage.success('书单删除成功')
  } catch (error) {
    console.error('删除失败:', error)
  }
}

const handleViewBookList = (bookList: any) => {
  router.push(`/booklist/${bookList.id}`)
}


// 获取笔记状态信息
const fetchNoteStatus = async (noteId: number) => {
  try {
    const [likeRes, payRes] = await Promise.all([
      getNoteLikeStatus(noteId),
      getNotePayStatus(noteId)
    ])
    return {
      isLiked: likeRes.data,
      isPaid: payRes.data
    }
  } catch (error) {
    console.error('获取笔记状态失败:', error)
    return { isLiked: false, isPaid: false }
  }
}

// 修改后的数据获取逻辑
onMounted(async () => {
  try {
    // 用户信息
    const userRes = await getUserInfo(userId.value)
    user.value = userRes.data.data || {}

    // 书单数据（含收藏状态）
    const listRes = await getUserBookLists(userId.value, 0, 10)
    booklists.value = await Promise.all(
        listRes.data.data?.content.map(async (list: any) => ({
          ...list,
          isFavourite: await fetchBookListStatus(list.id)
        })) || []
    )

    // 笔记数据（含状态）
    const noteRes = await getUserNotes(userId.value)
    notes.value = await Promise.all(
        noteRes.data.data.map(async (note: any) => ({
          ...note,
          ...(await fetchNoteStatus(note.id))
        }))
    )

  } catch (error) {
    console.error('数据加载失败:', error)
    ElMessage.error('加载失败，请检查控制台')
  } finally {
    loading.value = false
  }
})

// 事件处理
const handleLike = async (note: any) => {
  try {
    await (note.isLiked ?
        axios.post(`/api/note/like/sub/${note.id}`) :
        axios.post(`/api/note/like/add/${note.id}`))
    note.isLiked = !note.isLiked
    note.likeCnt += note.isLiked ? 1 : -1
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const handleUnlike = handleLike // 共用相同逻辑

const handleDeleteNote = async (id: number) => {
  try {
    await deleteNote(id)
    notes.value = notes.value.filter(note => note.id !== id)
    ElMessage.success('删除成功')
  } catch (error) {
    console.error('删除失败:', error)
  }
}

const handlePurchaseNote = async (note: any) => {
  try {
    await payNote(note.id)
    note.isPaid = true
    ElMessage.success('购买成功')
  } catch (error) {
    console.error('购买失败:', error)
  }
}

const handleViewNote = (note: any) => {
  router.push(`/note/${note.id}`)
}
</script>

<style scoped>
/* 保持原有样式 */
.profile {
  text-align: center;
  padding: 20px 0;

  .avatar {
    width: 120px;
    height: 120px;
    border-radius: 50%;
    margin-bottom: 15px;
  }

  h1 {
    margin: 10px 0;
    font-size: 24px;
  }
}

.booklist-item {
  margin: 12px 0;
  padding: 16px;
  border-radius: 8px;
  background: #f8f9fa;
  transition: all 0.3s;

  .title {
    font-weight: 500;
    margin-bottom: 8px;
  }

  .meta {
    margin-top: 8px;
    display: flex;
    gap: 8px;
    align-items: center;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  }
}

.empty-tip {
  padding: 40px 0;
}

/* 新增笔记列表间距 */
.el-tab-pane > div:not(.empty-tip) {
  display: block !important;
  gap: 16px;
}


/* 调整书单卡片样式 */
.booklist-card {
  margin-bottom: 16px;
}

/* 统一调整卡片间距 */
.el-tab-pane > div:not(.empty-tip) {
  gap: 12px; /* 原16px */
}

/* 调整卡片最小宽度 */
.booklist-card, .note-card {
  min-width: 280px; /* 原320px */
  max-width: 320px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .booklist-card, .note-card {
    min-width: 100%;
  }

  .el-tabs__content {
    padding: 8px; /* 原12px */
  }
}

/* 统一标签尺寸 */
.el-tag {
  height: 24px;
  padding: 0 8px;
  font-size: 12px;
}

/* 横向排列容器 */
.horizontal-list {
  display: flex;
  flex-wrap: wrap;
  gap: 24px; /* 项目间距 */
  padding: 12px 0;
}

/* 列表项通用样式 */
.list-item {
  flex: 0 0 calc(33.333% - 16px); /* 三列布局，留出间距 */
  min-width: 280px;
  max-width: 320px;
  transition: all 0.3s ease;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .list-item {
    flex: 0 0 calc(50% - 12px);  /* 中屏幕两列 */
  }
}

@media (max-width: 768px) {
  .list-item {
    flex: 0 0 100%;  /* 小屏幕单列 */
  }
}

/* 组件内部微调 */
.booklist-card,
.note-card {
  width: 100%;
  height: 100%;
  padding: 12px; /* 缩小内边距 */
}

.booklist-header h3 {
  font-size: 15px; /* 缩小标题 */
}

.description {
  font-size: 13px; /* 缩小描述文字 */
}
</style>
<script setup lang="ts">
import { Star, StarFilled, Delete } from '@element-plus/icons-vue'
import type { BookListVO } from '../api/booklist'

const props = defineProps<{
  bookList: BookListVO
  isFavourite: boolean
  isCreator: boolean
}>()

const emit = defineEmits<{
  (e: 'collect', bookList: BookListVO): void
  (e: 'delete', id: number): void
  (e: 'view', bookList: BookListVO): void
}>()

const handleCollect = () => {
  emit('collect', props.bookList)
}

const handleDelete = () => {
  emit('delete', props.bookList.id)
}

const handleView = () => {
  emit('view', props.bookList)
}
</script>

<template>
  <div class="booklist-card">
    <div class="booklist-header">
      <h3 @click="handleView" class="clickable">{{ bookList.title }}</h3>
      <div class="actions">
        <el-button
          :type="isFavourite ? 'warning' : 'default'"
          circle
          @click="handleCollect"
        >
          <el-icon>
            <StarFilled v-if="isFavourite" />
            <Star v-else />
          </el-icon>
        </el-button>
        <el-button
          v-if="isCreator"
          type="danger"
          circle
          @click="handleDelete"
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
</template>

<style scoped>
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

.clickable {
  cursor: pointer;
}

.clickable:hover {
  color: #409EFF;
}
</style> 
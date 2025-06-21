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
    <div class="booklist-cover" v-if="bookList.products && bookList.products.length > 0 && bookList.products[0].cover">
      <img :src="bookList.products[0].cover" :alt="bookList.title" class="cover-image" />
    </div>
    <div class="booklist-cover" v-else>
      <!-- 默认封面 -->
      <div class="default-cover">
        <span>{{ bookList.title }}</span>
      </div>
    </div>
    <div class="booklist-content">
      <div class="booklist-header">
        <h3 @click="handleView" class="booklist-title clickable">{{ bookList.title }}</h3>
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
      <p class="booklist-description">{{ bookList.description }}</p>
      <div class="booklist-footer">
        <div class="creator">
          <el-avatar :size="24" :src="bookList.creatorAvatar" />
          <span>{{ bookList.creatorName }}</span>
        </div>
        <div class="stats">
          <span>{{ bookList.products ? bookList.products.length : 0 }} 本书</span>
          <span>{{ bookList.favouriteCount }} 收藏</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.booklist-card {
  position: relative;
  height: 520px;
  border-radius: 16px;
  overflow: hidden;
  background: none;
  box-shadow: none;
  border: none;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: all 0.3s;
  margin: 16px;
  padding: 0;
}
.booklist-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 30px rgba(217, 83, 79, 0.15);
}
.booklist-cover {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}
.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
  border-radius: 0;
}
.default-cover {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #ff6347 0%, #d9534f 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  font-weight: bold;
  text-align: center;
  padding: 20px;
}
.booklist-card:hover .cover-image {
  transform: scale(1.05);
}
.booklist-content {
  position: absolute;
  left: 0; right: 0; bottom: 0;
  z-index: 2;
  padding: 32px 24px 24px 24px;
  background: linear-gradient(0deg, rgba(0,0,0,0.85) 60%, rgba(0,0,0,0.2) 100%, transparent 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.booklist-title {
  font-size: 20px;
  font-weight: bold;
  margin: 0 0 8px 0;
  color: #fff;
  text-shadow: 1px 1px 3px rgba(0,0,0,0.7);
}
.booklist-description {
  font-size: 15px;
  color: #fff;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.5);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.booklist-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  color: #fff;
  font-size: 13px;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.5);
}
.booklist-header, .actions, .description {
  position: relative;
  z-index: 3;
}
.clickable {
  cursor: pointer;
}
.clickable:hover {
  color: #409EFF;
}
.creator {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style> 
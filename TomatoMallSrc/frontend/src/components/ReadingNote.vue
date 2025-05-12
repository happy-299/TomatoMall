<!-- ReadingNote.vue -->
<script setup lang="ts">
import { Star, StarFilled, Delete } from '@element-plus/icons-vue'
import type { NoteVO } from '../api/note'

const props = defineProps<{
  note: NoteVO
  isLiked: boolean
  isCreator: boolean
  isPaid: boolean
}>()

const emit = defineEmits<{
  (e: 'like', note: NoteVO): void
  (e: 'unlike', note: NoteVO): void
  (e: 'delete', id: number): void
  (e: 'purchase', note: NoteVO): void
  (e: 'view', note: NoteVO): void
}>()

const handleLike = () => {
  emit('like', props.note)
}

const handleTitleClick = () => {
  emit('view', props.note)
}


const handleUnlike = () => {
  emit('unlike', props.note)
}

const handleDelete = () => {
  emit('delete', props.note.id)
}

const handlePurchase = () => {
  emit('purchase', props.note)
}
</script>

<template>
  <div class="note-card">
    <div class="note-header">
      <el-link
          type="primary"
          :underline="false"
          class="note-title"
          @click="handleTitleClick"
      >
        {{ note.title }}
      </el-link>
      <div class="actions">
        <el-button
            v-if="!isPaid && note.price > 0"
            type="success"
            size="small"
            @click="handlePurchase"
        >
          购买（¥{{ note.price }}）
        </el-button>
        <el-button
            :type="isLiked ? 'warning' : 'default'"
            circle
            @click="isLiked ? handleUnlike() : handleLike()"
        >
          <el-icon>
            <StarFilled v-if="isLiked" />
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
    <!-- 新增价格显示 -->
    <div class="note-price" :class="{ 'paid': isPaid }">
      <template v-if="note.price > 0">
        ¥{{ note.price }}
        <span v-if="isPaid" class="paid-badge">已购买</span>
      </template>
      <span v-else class="free">免费</span>
    </div>

    <div class="note-content">
      <img v-if="note.img" :src="note.img" class="note-image" alt="笔记封面" />
    </div>
    <div class="note-footer">
      <span>浏览量: {{ note.viewCnt }}</span>
      <span>点赞数: {{ note.likeCnt }}</span>
      <span v-if="note.price > 0">{{ isPaid ? '已购买' : '未购买' }}</span>
    </div>
  </div>
</template>

<style scoped>
.note-card {
  background: white;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s;
}

.note-card:hover {
  transform: translateY(-5px);
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.note-header h3 {
  margin: 0;
  font-size: 16px;
  color: #2c3e50;
}

.actions {
  display: flex;
  gap: 8px;
}

.note-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 4px;
  margin-bottom: 12px;
}

.note-content p {
  color: #666;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.note-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  font-size: 12px;
  color: #666;
}

.note-title {
  font-size: 18px;  /* 放大标题 */
  color: #333;      /* 默认黑色 */
  transition: color 0.3s;
}

.note-title:hover {
  color: #409EFF;   /* 悬停蓝色 */
}

.note-price {
  margin: 8px 0;
  font-size: 14px;
  color: #e6a23c;   /* 价格橙色 */
  display: flex;
  align-items: center;
  gap: 8px;
}

.note-price .paid {
  color: #67c23a;   /* 已购买绿色 */
}

.paid-badge {
  background: #f0f9eb;
  color: #67c23a;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.free {
  color: #909399;
}
</style>
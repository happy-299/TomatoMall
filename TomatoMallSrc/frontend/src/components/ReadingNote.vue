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
    <img v-if="note.img" :src="note.img" class="note-image" alt="笔记封面" />
    <div class="note-info">
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
            购买（🍅{{ note.price }}）
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
      <div class="note-price" :class="{ 'paid': isPaid }">
        <template v-if="note.price > 0">
          🍅{{ note.price }}
          <span v-if="isPaid" class="paid-badge">已购买</span>
        </template>
        <span v-else class="free">免费</span>
      </div>
      <div class="note-footer">
        <span>浏览量: {{ note.viewCnt }}</span>
        <span>点赞数: {{ note.likeCnt }}</span>
        <span v-if="note.price > 0">{{ isPaid ? '已购买' : '未购买' }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.note-card {
  position: relative;
  height: 520px;
  border-radius: 16px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  border: none;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: all 0.3s;
  padding: 0;
}
.note-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 30px rgba(217, 83, 79, 0.15);
}
.note-image {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 0;
  z-index: 1;
  transition: transform 0.3s;
}
.note-card:hover .note-image {
  transform: scale(1.05);
}
.note-info {
  position: absolute;
  left: 0; right: 0; bottom: 0;
  z-index: 2;
  padding: 32px 24px 24px 24px;
  background: transparent !important;
  color: #fff;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.note-title {
  font-size: 20px;
  font-weight: bold;
  margin: 0 0 8px 0;
  color: #fff;
  text-shadow: 1px 1px 3px rgba(0,0,0,0.7);
}
.note-price {
  margin: 8px 0 0 0;
  font-size: 15px;
  color: #ffe066;
  display: flex;
  align-items: center;
  gap: 8px;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.5);
}
.note-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  color: #fff;
  font-size: 13px;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.5);
}
.actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
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
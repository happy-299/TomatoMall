<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElRate, ElPagination, ElMessage, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElUpload, ElLoading, ElImageViewer, ElImage } from 'element-plus'
import { getReviews, getAverageRating, addReview, type Review, type PageResponse } from '../api/review'
import { getUserInfoById, type PartAccountVO } from '../api/user'
import { Plus, Picture } from '@element-plus/icons-vue'
import { uploadUserImage } from '../api/util.ts'
import {PRODUCT_MODULE} from "../api/_prefix.ts";

const props = defineProps<{
  productId: number
}>()

// 扩展Review类型以包含用户信息
interface ReviewWithUser extends Review {
  userInfo?: PartAccountVO
}

const reviews = ref<ReviewWithUser[]>([])
const currentPage = ref(1)
const pageSize = ref(5)
const total = ref(0)
const averageRating = ref(0)

// 评论表单相关
const dialogVisible = ref(false)
const rating = ref(0)
const content = ref('')
const images = ref<string[]>([])
const uploadRef = ref()

// 图片预览相关
const showViewer = ref(false)
const previewImageUrl = ref('')

const handleUploadSuccess = async (params: any) => {
  const loading = ElLoading.service({ fullscreen: false })
  try {
    const { file } = params
    console.log('开始上传图片:', file)
    const response = await uploadUserImage(file)
    console.log('图片上传响应:', response.data)
    
    if (response.data?.code === '200') {
      const imageUrl = response.data.data
      console.log('图片URL:', imageUrl)
      images.value.push(imageUrl)
      ElMessage.success('图片上传成功')
    } else {
      throw new Error(response.data?.msg || '图片上传失败')
    }
  } catch (error) {
    console.error('图片上传失败:', error)
    ElMessage.error('图片上传失败，请重试')
  } finally {
    loading.close()
  }
}

const handleRemoveImage = (uploadFile: any, uploadFiles: any) => {
  const index = uploadFiles.indexOf(uploadFile)
  if (index > -1) {
    images.value.splice(index, 1)
  }
}

const showReviewDialog = () => {
  dialogVisible.value = true
  rating.value = 0
  content.value = ''
  images.value = []
}

const submitReview = async () => {
  console.log('开始提交评论...')
  if (rating.value === 0) {
    ElMessage.warning('请选择评分')
    return
  }
  if (!content.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  try {
    console.log('准备发送请求，评分:', rating.value)
    console.log('用户ID:', sessionStorage.getItem('userId'))
    console.log('Token:', sessionStorage.getItem('token'))
    
    const response = await addReview(props.productId, {
      rating: Number(rating.value),
      content: content.value.trim(),
      images: images.value
    })
    
    console.log('收到响应:', response)
    
    if (response.data?.code === '200') {
      ElMessage.success('评论发表成功')
      dialogVisible.value = false
      // 刷新评论列表
      await fetchReviews()
      await fetchAverageRating()
    } else {
      ElMessage.error(response.data?.msg || '评论发表失败')
    }
  } catch (error: any) {
    console.error('评论发表失败:', error)
    if (error.message === '用户未登录') {
      ElMessage.error('请先登录后再发表评论')
    } else if (error?.response) {
      ElMessage.error(error.response.data?.msg || `评论发表失败 (${error.response.status})`)
    } else if (error?.request) {
      ElMessage.error('无法连接到服务器，请检查网络连接')
    } else {
      ElMessage.error('评论发表失败，请重试')
    }
  }
}

// 获取用户信息
const fetchUserInfo = async (userId: number): Promise<PartAccountVO | null> => {
  try {
    const response = await getUserInfoById(userId)
    if (response.data?.code === '200') {
      return response.data.data
    }
    return null
  } catch (error) {
    console.error('获取用户信息失败:', error)
    return null
  }
}

const fetchReviews = async () => {
  try {
    console.log('开始获取评论列表，页码:', currentPage.value - 1)
    const response = await getReviews(props.productId, currentPage.value - 1, pageSize.value)
    console.log('获取评论列表响应:', response.data)
    
    if (response.data?.code === '200') {
      const data = response.data.data
      const reviewsData = data.content || []
      
      // 为每个评论获取用户信息
      const reviewsWithUserInfo = await Promise.all(
        reviewsData.map(async (review: Review) => {
          const userInfo = review.userId ? await fetchUserInfo(review.userId) : null
          return {
            ...review,
            userInfo
          } as ReviewWithUser
        })
      )
      
      reviews.value = reviewsWithUserInfo
      total.value = data.total || 0
      console.log('评论列表数据:', {
        reviews: reviews.value,
        total: total.value
      })
    } else {
      console.error('获取评论列表失败:', response.data?.msg)
      ElMessage.error(response.data?.msg || '获取评论失败')
    }
  } catch (error) {
    console.error('获取评论列表异常:', error)
    ElMessage.error('获取评论失败')
  }
}

const fetchAverageRating = async () => {
  try {
    const response = await getAverageRating(props.productId)
    console.log('获取评分响应:', response)
    // 确保averageRating是一个数字
    averageRating.value = Number(response.data?.data || 0)
  } catch (error) {
    console.error('获取评分失败:', error)
    averageRating.value = 0
    ElMessage.error('获取评分失败')
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchReviews()
}

const handleDialogClose = () => {
  dialogVisible.value = false
  rating.value = 0
  content.value = ''
  images.value = []
}

const handleDialogConfirm = () => {
  submitReview()
}

const previewImage = (url: string) => {
  previewImageUrl.value = url
  showViewer.value = true
}

const handleImageError = (e: Event) => {
  const img = e.target as HTMLImageElement
  console.error('图片加载失败:', img.src)
  img.src = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZmlsbD0iIzkwOTM5OSIgZD0iTTIxIDE5VjVjMC0xLjEtLjktMi0yLTJINWMtMS4xIDAtMiAuOS0yIDJ2MTRjMCAxLjEuOSAyIDIgMmgxNGMxLjEgMCAyLS45IDItMnptLTIgMEg1VjVoMTR2MTR6bS0xMC0xMGMtMS4xIDAtMi0uOS0yLTJzLjktMiAyLTIgMiAuOSAyIDItLjkgMi0yIDJ6bTQgN2gtM1Y5aDN2N3oiLz48L3N2Zz4='
}

onMounted(() => {
  fetchReviews()
  fetchAverageRating()
  console.log('Token:', sessionStorage.getItem('token'))
  console.log('API URL:', `${PRODUCT_MODULE}/${props.productId}/reviews`)
})
</script>

<template>
  <div class="review-section">
    <div class="review-header">
      <div class="rating-summary">
        <span class="rating-label">商品评分：</span>
        <el-rate
          :model-value="averageRating"
          :max="10"
          disabled
          :colors="['#272643', '#272643', '#272643']"
        />
        <span class="rating-value">{{ Number(averageRating).toFixed(1) }}</span>
      </div>
      <el-button type="primary" @click="showReviewDialog">发表评论</el-button>
    </div>

    <div class="reviews-list">
      <div v-if="reviews.length === 0" class="no-reviews">
        暂无评论
      </div>
      <div v-else v-for="review in reviews" :key="review.id || Math.random()" class="review-item">
        <div class="review-header">
          <el-rate
            :model-value="review.rating"
            :max="10"
            disabled
            :colors="['#272643', '#272643', '#272643']"
          />
          <span class="review-time">{{ review.createTime ? new Date(review.createTime).toLocaleString() : '' }}</span>
        </div>
        
        <div class="review-content-wrapper">
          <div class="review-content">{{ review.content }}</div>
          
          <!-- 用户信息显示在右侧 -->
          <div v-if="review.userInfo" class="user-info">
            <div class="user-avatar">
              <el-image
                :src="review.userInfo.avatar"
                class="avatar-img"
                fit="cover"
              >
                <template #error>
                  <div class="avatar-error">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
            </div>
            <div class="user-details">
              <span class="username">{{ review.userInfo.username }}</span>
              <span v-if="review.userInfo.isVerified" class="verified-badge">
                {{ review.userInfo.verifiedName }}
              </span>
            </div>
          </div>
        </div>
        
        <div v-if="review.reviewImgs && review.reviewImgs.length > 0" class="review-images">
          <div v-for="(image, index) in review.reviewImgs" :key="index" class="image-container">
            <img
              :src="image"
              class="review-image"
              @click="previewImage(image)"
              @error="handleImageError"
            />
          </div>
        </div>
      </div>
    </div>

    <el-pagination
      v-if="total > 0"
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      @current-change="handlePageChange"
      layout="prev, pager, next"
      class="pagination"
    />

    <!-- 评论对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="发表评论"
      width="500px"
      @close="handleDialogClose"
    >
      <el-form>
        <el-form-item label="评分">
          <el-rate
            v-model="rating"
            :max="10"
            :colors="['#272643', '#272643', '#272643']"
          />
        </el-form-item>
        <el-form-item label="评论内容">
          <el-input
            v-model="content"
            type="textarea"
            :rows="4"
            placeholder="请输入您的评论内容"
          />
        </el-form-item>
        <el-form-item label="上传图片">
          <el-upload
            ref="uploadRef"
            :auto-upload="true"
            :show-file-list="true"
            :http-request="handleUploadSuccess"
            :on-remove="handleRemoveImage"
            list-type="picture-card"
            :limit="9"
            accept="image/jpeg,image/png,image/gif"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleDialogClose">取消</el-button>
          <el-button type="primary" @click="handleDialogConfirm">发表</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 图片预览对话框 -->
    <el-dialog
      v-model="showViewer"
      title="图片预览"
      width="80%"
    >
      <el-image
        :src="previewImageUrl"
        :preview-src-list="[previewImageUrl]"
        fit="contain"
      />
    </el-dialog>
  </div>
</template>

<style scoped>
.review-section {
  margin-top: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.rating-summary {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rating-label {
  color: #272643;
  font-weight: 500;
}

.rating-value {
  color: #ff4d4f;
  font-weight: bold;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  padding: 12px;
  background: white;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.review-time {
  color: #909399;
  font-size: 12px;
}

.review-content-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 12px;
}

.review-content {
  color: #272643;
  line-height: 1.6;
  font-size: 14px;
  flex: 1;
  margin: 0;
}

.review-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.image-container {
  position: relative;
  width: 80px;
  height: 80px;
  overflow: hidden;
  border-radius: 4px;
  cursor: pointer;
  background-color: #f5f7fa;
}

.review-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.2s;
}

.review-image:hover {
  transform: scale(1.05);
}

.image-error {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
  color: #909399;
  font-size: 12px;
}

.image-error .el-icon {
  font-size: 24px;
  margin-bottom: 4px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.no-reviews {
  text-align: center;
  color: #909399;
  padding: 20px;
  background: white;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  min-width: 80px;
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  border: 2px solid #f0f0f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
  color: #909399;
  font-size: 16px;
}

.user-details {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  text-align: center;
}

.username {
  font-weight: 600;
  color: #2c3e50;
  font-size: 12px;
  line-height: 1.2;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.verified-badge {
  background: linear-gradient(135deg, #ffd666 0%, #ffc53d 100%);
  color: #272643;
  padding: 2px 6px;
  border-radius: 8px;
  font-size: 9px;
  font-weight: 500;
  line-height: 1;
  text-align: center;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .review-content-wrapper {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .user-info {
    flex-direction: row;
    align-items: center;
    gap: 8px;
    min-width: auto;
  }
  
  .user-avatar {
    width: 36px;
    height: 36px;
  }
  
  .user-details {
    align-items: flex-start;
    text-align: left;
  }
  
  .username {
    font-size: 13px;
    max-width: 120px;
  }
  
  .verified-badge {
    font-size: 10px;
    max-width: 120px;
  }
}

@media (max-width: 480px) {
  .review-item {
    padding: 10px;
  }
  
  .review-content {
    font-size: 13px;
  }
  
  .user-avatar {
    width: 32px;
    height: 32px;
  }
  
  .username {
    font-size: 12px;
    max-width: 100px;
  }
  
  .verified-badge {
    font-size: 9px;
    max-width: 100px;
  }
}
</style> 
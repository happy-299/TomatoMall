<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ElCard, ElMessage, ElButton, ElRate, ElMessageBox,
  ElDialog, ElForm, ElFormItem, ElInput, ElInputNumber, ElTag, ElLoading
} from 'element-plus'
import { getProductById, deleteProduct, updateProduct, type Product, type Specification } from '../../api/product'
import { getUserInfo } from "../../api/user.ts";
import { uploadUserImage } from '../../api/util'

const route = useRoute()
const router = useRouter()
const product = ref<Product | null>(null)
const loading = ref(true)
const isAdmin = ref(false)
const editDialogVisible = ref(false)

// 编辑表单数据
const editForm = ref({
  id: '',
  title: '',
  price: 0,
  rate: 0,
  description: '',
  cover: '',
  detail: '',
  specifications: [] as Specification[]
})

const handleCoverUpload = async (params: any) => {
  const loading = ElLoading.service({ fullscreen: false });
  try {
    const { file } = params;
    const response = await uploadUserImage(file);
    editForm.value.cover = response.data.data; // 更新封面URL
    ElMessage.success('封面图片上传成功');
  } catch (error) {
    ElMessage.error('封面图片上传失败，请重试');
  } finally {
    loading.close();
  }
};

// 验证规则
const rules = {
  title: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  price: [
    { required: true, message: '请输入商品价格', trigger: 'blur' },
    { type: 'number', min: 0, message: '价格不能小于0', trigger: 'change' }
  ],
  cover: [{ required: true, message: '请上传图片', trigger: 'blur' }]
}

// 获取商品详情
const fetchProduct = async () => {
  try {
    const res = await getProductById(route.params.id as string);
    if (res.data?.code === "200") {
      product.value = res.data.data;
      initEditForm(); // 初始化编辑表单
    } else {
      throw new Error(res.data?.msg || '无效的响应结构');
    }
  } catch (error: any) {
    ElMessage.error(`加载失败: ${error.message}`);
  } finally {
    loading.value = false;
  }
};

// 初始化编辑表单
const initEditForm = () => {
  if (product.value) {
    editForm.value = {
      id: product.value.id,
      title: product.value.title,
      price: product.value.price,
      rate: product.value.rate,
      description: product.value.description || '',
      cover: product.value.cover || '',
      detail: product.value.detail || '',
      specifications: product.value.specifications?.map(spec => ({
        ...spec
      })) || []
    }
  }
}

// 打开编辑对话框
const openEditDialog = () => {
  editDialogVisible.value = true
}

// 提交修改
const handleEditSubmit = async () => {
  try {
    if (!product.value) return

    await updateProduct(editForm.value)
    ElMessage.success('商品更新成功')
    editDialogVisible.value = false
    await fetchProduct() // 刷新数据
  } catch (error) {
    ElMessage.error('商品更新失败')
  }
}

// 规格操作
const handleAddSpec = () => {
  editForm.value.specifications.push({
    id: Date.now().toString(),
    item: '',
    value: '',
    productId: product.value?.id || ''
  })
}

const handleRemoveSpec = (index: number) => {
  editForm.value.specifications.splice(index, 1)
}

// 删除商品
const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除该商品吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteProduct(route.params.id as string)
    ElMessage.success('删除成功')
    router.push('/productList')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 初始化
onMounted(async () => {
  try {
    // 检查管理员权限
    const username = sessionStorage.getItem('username')
    if (username) {
      const res = await getUserInfo(username)
      isAdmin.value = res.data.data?.role === 'admin'
    }

    // 获取商品数据
    await fetchProduct()
  } catch (error) {
    ElMessage.error('初始化失败')
  }
})
</script>

<template>
  <div class="product-detail-container">
    <el-card v-loading="loading" class="product-card">
      <!-- 管理员操作 -->
      <div v-if="isAdmin" class="admin-actions">
        <el-button type="primary" @click.stop="openEditDialog"
                   color="#bae8e8">
          编辑商品
        </el-button>
        <el-button type="danger" @click.stop="handleDelete">
          删除商品
        </el-button>
      </div>

      <!-- 编辑对话框 -->
      <el-dialog
          v-model="editDialogVisible"
          title="编辑商品信息"
          width="800px"
          :close-on-click-modal="false"
      >
        <el-form
            :model="editForm"
            :rules="rules"
            label-width="100px"
            label-position="top"
        >
          <el-form-item label="商品名称" prop="title">
            <el-input v-model="editForm.title" />
          </el-form-item>

          <el-form-item label="价格" prop="price">
            <el-input-number
                v-model="editForm.price"
                :min="0"
                :precision="2"
                controls-position="right"
            />
          </el-form-item>

          <el-form-item label="商品评分">
            <el-rate
                v-model="editForm.rate"
                :max="10"
                :colors="['#272643', '#272643', '#272643']"
                allow-half
            />
          </el-form-item>

          <el-form-item label="封面图片" prop="cover">
            <el-upload
                :auto-upload="true"
                :http-request="handleCoverUpload"
                :show-file-list="false"
            >
              <template #trigger>
                <el-button type="primary">上传封面</el-button>
              </template>
              <div class="cover-preview" v-if="editForm.cover">
                <img
                    :src="editForm.cover"
                    class="preview-image"
                    alt="封面预览"
                />
                <div class="preview-tip">（点击上方按钮重新上传）</div>
              </div>
              <template #tip>
                <div class="upload-tip">支持JPG/PNG格式，建议尺寸800x800px</div>
              </template>
            </el-upload>
          </el-form-item>

          <el-form-item label="商品描述">
            <el-input
                v-model="editForm.description"
                type="textarea"
                :rows="4"
            />
          </el-form-item>

          <el-form-item label="商品详情">
            <el-input
                v-model="editForm.detail"
                type="textarea"
                :rows="6"
            />
          </el-form-item>

          <el-form-item label="商品规格">
            <div class="spec-editor">
              <div
                  v-for="(spec, index) in editForm.specifications"
                  :key="spec.id"
                  class="spec-item"
              >
                <el-input
                    v-model="spec.item"
                    placeholder="规格名称"
                    class="spec-input"
                />
                <el-input
                    v-model="spec.value"
                    placeholder="规格值"
                    class="spec-input"
                />
                <el-button
                    type="danger"
                    circle
                    @click="handleRemoveSpec(index)"
                >
                  ×
                </el-button>
              </div>
              <el-button
                  type="primary"
                  plain
                  @click="handleAddSpec"
              >
                添加规格
              </el-button>
            </div>
          </el-form-item>
        </el-form>

        <template #footer>
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleEditSubmit">保存修改</el-button>
        </template>
      </el-dialog>

      <!-- 商品内容 -->
      <div v-if="product" class="product-content">
        <div class="cover-container">
          <img
              v-if="product.cover"
              :src="product.cover"
              class="product-cover"
              @error="(e: any) => e.target.style.display = 'none'"
          >
          <div v-else class="cover-placeholder">暂无图片</div>
        </div>

        <div class="product-info">
          <h1 class="title">{{ product.title }}</h1>

          <div class="meta-section">
            <div class="price-rate">
              <span class="price">¥{{ product.price.toFixed(2) }}</span>
              <el-rate
                  v-model="product.rate"
                  :max="10"
                  disabled
                  :colors="['#272643', '#272643', '#272643']"
              />
            </div>

            <div v-if="product.specifications?.length" class="specifications">
              <h3>产品规格</h3>
              <div class="spec-grid">
                <div
                    v-for="spec in product.specifications"
                    :key="spec.id"
                    class="spec-item"
                >
                  <span class="spec-label">{{ spec.item }}：</span>
                  <el-tag type="info" class="spec-value">{{ spec.value }}</el-tag>
                </div>
              </div>
            </div>

            <div class="description-section">
              <div class="description">
                <h3>商品描述</h3>
                <p>{{ product.description || '暂无详细描述' }}</p>
              </div>

              <div v-if="product.detail" class="detail">
                <h3>商品详情</h3>
                <p class="detail-content">{{ product.detail }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.product-detail-container {
  padding: 24px;
  background: linear-gradient(120deg, #e3f6f5 0%, #d0eeff 100%);
  min-height: 100vh;
}

.product-card {
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
  border-radius: 12px;
}

.admin-actions {
  position: absolute;
  right: 20px;
  top: 20px;
  z-index: 1;
  display: flex;
  gap: 10px;
}

.product-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  padding: 24px;
}

.cover-container {
  position: relative;
  height: 0;
  padding-bottom: 100%;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fa;
}

.product-cover {
  position: absolute;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  position: absolute;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 16px;
  background: #f0f2f5;
}

.product-info {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.title {
  color: #272643;
  font-size: 28px;
  margin: 0;
}

.price-rate {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.price {
  color: #ff4d4f;
  font-size: 32px;
  font-weight: bold;
}

.spec-editor {
  width: 100%;
}

.spec-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.spec-input {
  flex: 1;
}

.specifications {
  margin-top: 20px;
}

.spec-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 10px;
}

.spec-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.spec-label {
  color: #606266;
}

.spec-value {
  flex-shrink: 0;
}

.description-section {
  margin-top: 24px;
}

.description {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
}

.description h3 {
  color: #272643;
  margin: 0 0 12px 0;
  font-size: 18px;
}

.detail-content {
  white-space: pre-wrap;
}

@media (max-width: 768px) {
  .product-content {
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .title {
    font-size: 24px;
  }

  .price {
    font-size: 28px;
  }
}

.cover-preview {
  margin-top: 10px;
  text-align: center;
}

.preview-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: 6px;
  margin: 10px 0;
}

.preview-tip {
  color: #909399;
  font-size: 12px;
}

.upload-tip {
  color: #2c698d;
  font-size: 12px;
  margin-top: 8px;
}
</style>

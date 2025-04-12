<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  ElCard, ElMessage, ElButton, ElRate, ElDialog,
  ElForm, ElFormItem, ElInput, ElInputNumber, ElLoading
} from 'element-plus'
import {
  getProducts,
  deleteProduct,
  createProduct,
  adjustStockpile,
  getStockpile,
  type Product,
  type Stockpile, Specification
} from '../../api/product'
import { useRouter } from 'vue-router'
import {getUserInfo} from "../../api/user.ts";
import {uploadUserImage} from "../../api/util.ts";
import {getCart, addToCart, updateCartItemQuantity, type CartItem, deleteCartItem} from '../../api/cart'
import { ShoppingCart } from '@element-plus/icons-vue'

const router = useRouter()
const products = ref<Product[]>([])
const stockpiles = ref<Record<string, Stockpile>>({})
const isAdmin = ref(!!sessionStorage.getItem('token'))
const dialogVisible = ref(false)
const stockDialogVisible = ref(false)
const currentProduct = ref<Product | null>(null)
const cartItems = ref<Record<string, { cartItemId: string; quantity: number }>>({})
const loadingCart = ref(false)

// 表单结构
const formDefaults = {
  title: '',
  price: 0,
  cover: '',
  rate: 0,
  description: '',
  detail:'',
  specifications: [] as Specification[] // 新增规格字段
}

const form = reactive({ ...formDefaults })
const stockForm = reactive({
  amount: 0,
  frozen: 0
})

// 新增规格操作方法
const addSpecification = () => {
  form.specifications.push({ item: '', value: '' });
}

const removeSpecification = (index: number) => {
  form.specifications.splice(index, 1);
}

const handleCoverUpload = async (params: any) => {
  const loading = ElLoading.service({ fullscreen: false });
  try {
    const { file } = params;
    const response = await uploadUserImage(file);
    form.cover = response.data.data; // 更新封面URL
    ElMessage.success('封面图片上传成功');
  } catch (error) {
    ElMessage.error('封面图片上传失败，请重试');
  } finally {
    loading.close();
  }
};

// 获取购物车数据
const fetchCart = async () => {
  try {
    loadingCart.value = true
    const res = await getCart()
    console.log("getCart => ",res)

    // 增加业务状态码检查
    if (res.data.code !== '200') { // 注意code是字符串类型
      throw new Error(res.data.msg || '获取购物车数据失败')
    }

    // 安全访问数据
    const cartData = res.data.data || {}
    cartData.items?.forEach((item: CartItem) => {
      cartItems.value[item.productId] = {
        cartItemId: item.cartItemId,
        quantity: item.quantity
      }
    })

  } catch (error: any) {
    ElMessage.error(error.message || '获取购物车失败')
  } finally {
    loadingCart.value = false
  }
}

// 处理购物车操作
const handleCart = async (productId: string, type: 'add' | 'subtract') => {
  try {
    const currentItem = cartItems.value[productId]
    if (!currentItem) {
      // 如果商品不在购物车中，尝试添加
      if (type === 'add') {
        await addToCart(productId, 1)
        await fetchCart() // 重新获取购物车数据以获取cartItemId
      }
      return
    }

    const currentQuantity = currentItem.quantity
    const stock = stockpiles.value[productId]?.amount || 0
    let newQuantity = currentQuantity

    if (type === 'add') {
      if (currentQuantity >= stock) {
        return ElMessage.warning('库存不足')
      }
      newQuantity = currentQuantity + 1
    } else {
      newQuantity = Math.max(0, currentQuantity - 1)
    }

    // 处理数量变化
    if (newQuantity === 0) {
      // 删除商品项
      await deleteCartItem(currentItem.cartItemId)
      delete cartItems.value[productId]
    } else {
      // 更新数量
      await updateCartItemQuantity(currentItem.cartItemId, newQuantity)
      cartItems.value[productId].quantity = newQuantity
    }

    // 立即更新UI
    if (newQuantity > 0) {
      cartItems.value[productId].quantity = newQuantity
    }
  } catch (error) {
    ElMessage.error('操作失败，请重试')
    console.error('购物车操作错误:', error)
  }
}

// 验证规则
const rules = {
  title: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  price: [
    { required: true, message: '请输入商品价格', trigger: 'blur' },
    { type: 'number', min: 0, message: '价格不能小于0', trigger: 'change' }
  ],
  cover: [{ required: true, message: '请上传图片', trigger: 'blur' }]
}

// 获取商品列表及库存
const fetchProducts = async () => {
  try {
    const res = await getProducts();
    products.value = res.data.data;

    await Promise.all(products.value.map(async product => {
      try {
        const stockRes = await getStockpile(product.id);
        const code = stockRes.data.code.toString(); // 统一类型

        if (code === '200') {
          stockpiles.value[product.id] = stockRes.data.data;
        } else if (code === '404') {
          // 库存不存在时初始化
          stockpiles.value[product.id] = {
            id: '',
            productId: product.id,
            amount: 0,
            frozen: 0
          };
        } else {
          // 明确错误信息
          const errorMsg = stockRes.data.msg || `库存异常 (CODE: ${code})`;
          throw new Error(errorMsg);
        }
      } catch (error) {
        console.error(`商品 ${product.title} 库存处理失败:`, error);
        // 初始化默认库存
        stockpiles.value[product.id] = {
          id: '',
          productId: product.id,
          amount: 0,
          frozen: 0
        };
      }
    }));
  } catch (error) {
    ElMessage.error('商品列表加载失败');
  }
};

// 打开库存编辑弹窗
const openStockDialog = async (product: Product) => {
  try {
    currentProduct.value = product
    const res = await getStockpile(product.id)
    stockForm.amount = res.data.data.amount
    stockForm.frozen = res.data.data.frozen // 新增冻结库存初始化
    stockDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取库存信息失败')
  }
}


// 提交库存修改
const handleStockUpdate = async () => {
  try {
    if (!currentProduct.value) return

    // 1. 提交库存修改
    await adjustStockpile(currentProduct.value.id, stockForm.amount)

    // 2. 获取最新库存（强制刷新）
    const res = await getStockpile(currentProduct.value.id)

    // 3. 正确更新响应式数据
    stockpiles.value = {
      ...stockpiles.value,
      [currentProduct.value.id]: res.data.data // 根据实际API响应调整
    }

    ElMessage.success('库存更新成功')
    stockDialogVisible.value = false
  } catch (error) {
    console.error('库存更新失败:', error)
    ElMessage.error('库存更新失败')
  }
}

// 商品创建
const submitForm = async () => {
  try {
    // 过滤空规格
    const validSpecs = form.specifications.filter(s => s.item.trim() && s.value.trim());

    await createProduct({
      ...form,
      price: Number(form.price),
      specifications: validSpecs
    });

    ElMessage.success('创建成功');
    dialogVisible.value = false;
    Object.assign(form, formDefaults);
    await fetchProducts();
  } catch (error) {
    ElMessage.error('创建失败');
  }
}

// 商品删除
const handleDelete = async (id: string) => {
  try {
    await deleteProduct(id)
    ElMessage.success('删除成功')
    await fetchProducts()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

onMounted(async () => {
  // 先获取用户信息
  try {
    const username = sessionStorage.getItem('username')
    if (username) {
      const res = await getUserInfo(username)
      isAdmin.value = res.data.data?.role === 'admin'
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户权限失败')
  }

  // 然后获取商品列表
  await fetchProducts()
  await fetchCart()
})

</script>

<template>
  <div class="product-list-container">
    <!-- 头部 -->
    <div class="header">
      <h1>商品列表</h1>
      <el-button v-if="isAdmin" type="primary" @click="dialogVisible = true">
        新建商品
      </el-button>
    </div>

    <!-- 新建商品弹窗 -->
    <el-dialog
        v-model="dialogVisible"
        title="新建商品"
        width="600px"
        :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="rules" label-width="100px">
        <el-form-item label="商品名称" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>

        <el-form-item label="价格" prop="price">
          <el-input-number
              v-model="form.price"
              :min="0"
              :precision="2"
              controls-position="right"
          />
        </el-form-item>

        <el-form-item label="商品评分">
          <el-rate
              v-model="form.rate"
              :max="10"
              :colors="['#272643', '#272643', '#272643']"
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
            <div class="cover-preview" v-if="form.cover">
              <img
                  :src="form.cover"
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
              v-model="form.description"
              type="textarea"
              :rows="4"
          />
        </el-form-item>

        <el-form-item label="商品详情">
          <el-input
              v-model="form.detail"
              type="textarea"
              :rows="6"
          />
        </el-form-item>

        <el-form-item label="商品规格">
          <div class="specifications">
            <div
                v-for="(spec, index) in form.specifications"
                :key="index"
                class="spec-item"
            >
              <el-input
                  v-model="spec.item"
                  placeholder="规格"
                  style="width: 200px; margin-right: 10px;"
              />
              <el-input
                  v-model="spec.value"
                  placeholder="值"
                  style="width: 250px; margin-right: 10px;"
              />
              <el-button
                  type="danger"
                  circle
                  @click="removeSpecification(index)"
              >
                ×
              </el-button>
            </div>
            <el-button
                type="primary"
                plain
                @click="addSpecification"
            >
              添加规格
            </el-button>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">创建商品</el-button>
      </template>
    </el-dialog>

    <!-- 库存修改弹窗 -->
    <el-dialog
        v-model="stockDialogVisible"
        title="修改库存"
        width="400px"
    >
      <el-form :model="stockForm">
        <el-form-item label="库存数量" label-width="80px">
          <el-input-number
              v-model="stockForm.amount"
              :min="0"
              controls-position="right"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStockUpdate">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 商品列表 -->
    <div class="grid-container">
      <el-card
          v-for="product in products"
          :key="product.id"
          class="product-card"
          @click="router.push(`/product/${product.id}`)"
      >
        <img v-if="product.cover" :src="product.cover" class="product-cover" />
        <div class="product-info">
          <h3 class="title">{{ product.title }}</h3>

          <div class="price-rate">
            <div class="price-section">
              <span class="price">¥{{ product.price.toFixed(2) }}</span>
              <div class="stock-info">
                <span>库存: {{ stockpiles[product.id]?.amount || 0 }}</span>
                <span class="frozen">(冻结: {{ stockpiles[product.id]?.frozen || 0 }})</span>
              </div>
            </div>
            <el-rate
                v-model="product.rate"
                :max="10"
                disabled
                :colors="['#272643', '#272643', '#272643']"
            />
          </div>

          <div v-if="isAdmin" class="admin-actions">
            <el-button
                size="small"
                type="primary"
                @click.stop="openStockDialog(product)"
                color="#bae8e8"
            >
              改库存
            </el-button>
            <el-button
                type="danger"
                size="small"
                @click.stop="handleDelete(product.id)"
            >
              删除
            </el-button>
          </div>
          <div class="user-actions">
            <el-button
                type="primary"
                class="buy-btn"
                @click.stop="router.push('/pay')"
            >
              立即购买
            </el-button>

            <div class="cart-operations">
              <template v-if="cartItems[product.id]?.quantity > 0">
                <el-button
                    circle
                    size="small"
                    @click.stop="handleCart(product.id, 'subtract')"
                >
                  -
                </el-button>
                <span class="quantity">{{ cartItems[product.id]?.quantity }}</span>
                <el-button
                    circle
                    size="small"
                    :disabled="cartItems[product.id]?.quantity >= stockpiles[product.id]?.amount"
                    @click.stop="handleCart(product.id, 'add')"
                >
                  +
                </el-button>
              </template>
              <el-button
                  v-else
                  :icon="ShoppingCart"
                  circle
                  type="info"
                  size="small"
                  @click.stop="handleCart(product.id, 'add')"
              />
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>



.product-list-container {
  padding: 24px;
  background: linear-gradient(120deg, #e3f6f5 0%, #d0eeff 100%);
  min-height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 0 20px;
}

.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  padding: 0 20px;
}

.product-card {
  cursor: pointer;
  transition: transform 0.3s;
  border-radius: 8px;
}

.product-card:hover {
  transform: translateY(-5px);
}

.product-cover {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 8px 8px 0 0;
}

.product-info {
  padding: 12px;
}

.title {
  color: #272643;
  margin: 8px 0;
  height: 44px;
  overflow: hidden;
}

.price-rate {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.price-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  color: #ff4d4f;
  font-size: 18px;
  font-weight: bold;
}

.stock-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  font-size: 12px;
}

.frozen {
  color: #909399;
}

.admin-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

:deep(.el-dialog__body) {
  padding: 20px 25px;
}

.el-form-item {
  margin-bottom: 18px;
}

.el-input-number {
  width: 100%;
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
.user-actions {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.buy-btn {
  background: #2c698d;
  border-color: #2c698d;
  color: white;
  transition: all 0.3s;
}

.buy-btn:hover {
  opacity: 0.9;
  transform: translateY(-2px);
}

.cart-operations {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quantity {
  min-width: 24px;
  text-align: center;
  color: #2c698d;
  font-weight: 500;
}

:deep(.el-button.is-circle) {
  width: 28px;
  height: 28px;
  padding: 0;
}
</style>
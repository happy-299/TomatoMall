<script setup lang="ts">
import {ref, reactive, onMounted, computed, onUnmounted} from 'vue'
import {
  ElMessage, ElButton, ElRate, ElDialog,
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
import {getUserInfo} from "../../api/user.ts";
import {uploadUserImage} from "../../api/util.ts";
import AdCarousel from '../../components/AdCarousel.vue'
import {
  Advertisement,
  updateAdvertisement,
  createAdvertisement,
  deleteAdvertisement,
  getAdvertisements
} from '../../api/advertisement'
import {getCart, addToCart, updateCartItemQuantity, type CartItem, deleteCartItem} from '../../api/cart'
import { Search } from '@element-plus/icons-vue'
import { getSearchHistory, search, type SearchResult, type SearchHistoryItem } from '../../api/search'


//const router = useRouter()
const products = ref<Product[]>([])
const stockpiles = ref<Record<string, Stockpile>>({})
const isAdmin = ref(!!sessionStorage.getItem('token'))
const dialogVisible = ref(false)
const stockDialogVisible = ref(false)
const currentProduct = ref<Product | null>(null)

const cartItems = ref<Record<string, { cartItemId: string; quantity: number }>>({})
const loadingCart = ref(false)
const editAdDialogVisible = ref(false)
const editAdForm = reactive({
  id: '',
  title: '',
  content: '',
  imgUrl: '',
  productId: ''
})

// 表单结构
const formDefaults = {
  title: '',
  price: 0,
  cover: '',
  rate: 0,
  description: '',
  detail: '',
  specifications: [] as Specification[] // 新增规格字段
}

const ads = ref<Advertisement[]>([])
const adDialogVisible = ref(false)
const currentAdProductId = ref('')
const adForm = reactive({
  title: '',
  content: '',
  imgUrl: '',
  productId: ''
})

// 修改后的计算属性
const hasAdvertisement = computed(() => (productId: string) => {
  return ads.value.some(ad => ad.productId === productId)
})

const fetchAds = async () => {
  try {
    const res = await getAdvertisements()
    ads.value = res.data.data
  } catch (error) {
    console.error('获取广告失败:', error)
  }
}

// 修改handleAdClick方法，添加loading状态和错误处理
const handleAdClick = async (productId: string) => {
  const loading = ElLoading.service({fullscreen: true})
  try {
    if (hasAdvertisement.value(productId)) {
      const adIndex = ads.value.findIndex(ad => ad.productId === productId)
      if (adIndex > -1) {
        await deleteAdvertisement(ads.value[adIndex].id)
        // 使用splice保持响应式更新
        ads.value.splice(adIndex, 1)
        ElMessage.success('广告删除成功')
      }
    } else {
      adForm.productId = productId
      adDialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    loading.close()
  }
}

const handleAdImageUpload = async (params: any) => {
  const loading = ElLoading.service({fullscreen: false})
  try {
    const {file} = params
    const response = await uploadUserImage(file)
    adForm.imgUrl = response.data.data
    ElMessage.success('图片上传成功')
  } catch (error) {
    ElMessage.error('图片上传失败')
  } finally {
    loading.close()
  }
}

// 修改createNewAd方法
const createNewAd = async () => {
  const loading = ElLoading.service({fullscreen: true})
  try {
    const res = await createAdvertisement(adForm)
    // 使用unshift使新广告立即显示
    ads.value.unshift(res.data.data)
    ElMessage.success('广告创建成功')
    adDialogVisible.value = false
    Object.assign(adForm, {
      title: '',
      content: '',
      imgUrl: '',
      productId: ''
    })
  } catch (error) {
    ElMessage.error('创建失败')
  } finally {
    loading.close()
  }
}

const form = reactive({...formDefaults})
const stockForm = reactive({
  amount: 0,
  frozen: 0
})

const openEditAdDialog = (productId: string) => {
  const ad = ads.value.find(ad => ad.productId === productId)
  if (ad) {
    editAdForm.id = ad.id
    editAdForm.title = ad.title
    editAdForm.content = ad.content
    editAdForm.imgUrl = ad.imgUrl
    editAdForm.productId = ad.productId
    editAdDialogVisible.value = true
  }
}

// 更新广告方法
const updateAd = async () => {
  const loading = ElLoading.service({fullscreen: true})
  try {
    await updateAdvertisement(editAdForm)
    // 更新本地广告数据
    const index = ads.value.findIndex(ad => ad.id === editAdForm.id)
    if (index > -1) {
      ads.value[index] = {...ads.value[index], ...editAdForm}
    }
    ElMessage.success('广告更新成功')
    editAdDialogVisible.value = false
  } catch (error) {
    ElMessage.error('更新失败')
  } finally {
    loading.close()
  }
}

const handleEditAdImageUpload = async (params: any) => {
  const loading = ElLoading.service({ fullscreen: false })
  try {
    const { file } = params
    const response = await uploadUserImage(file)
    editAdForm.imgUrl = response.data.data  // 更新到编辑表单
    ElMessage.success('图片上传成功')
  } catch (error) {
    ElMessage.error('图片上传失败')
  } finally {
    loading.close()
  }
}

// 新增规格操作方法
const addSpecification = () => {
  form.specifications.push({item: '', value: '',id:'',productId:''});
}

const removeSpecification = (index: number) => {
  form.specifications.splice(index, 1);
}

const handleCoverUpload = async (params: any) => {
  const loading = ElLoading.service({fullscreen: false});
  try {
    const {file} = params;
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
    console.log("getCart => ", res)

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
  title: [{required: true, message: '请输入商品名称', trigger: 'blur'}],
  price: [
    {required: true, message: '请输入商品价格', trigger: 'blur'},
    {type: 'number', min: 0, message: '价格不能小于0', trigger: 'change'}
  ],
  cover: [{required: true, message: '请上传图片', trigger: 'blur'}]
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

// 添加搜索相关的状态
const searchKeyword = ref('')
const searchHistory = ref<SearchHistoryItem[]>([])
const isSearching = ref(false)
const showHistory = ref(false)

// 获取搜索历史
const fetchSearchHistory = async () => {
  try {
    const res = await getSearchHistory()
    if (res.data.code === '200') {
      searchHistory.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取搜索历史失败:', error)
  }
}

// 处理搜索框点击
const handleSearchFocus = () => {
  showHistory.value = true
}

// 处理点击外部关闭历史记录
const handleClickOutside = (event: MouseEvent) => {
  const searchContainer = document.querySelector('.search-container')
  if (searchContainer && !searchContainer.contains(event.target as Node)) {
    showHistory.value = false
  }
}

// 搜索方法
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    await fetchProducts() // 如果搜索关键词为空，显示所有商品
    return
  }

  isSearching.value = true
  showHistory.value = false // 搜索时隐藏历史记录
  try {
    const res = await search(searchKeyword.value)
    if (res.data.code === '200') {
      const searchResult: SearchResult = res.data.data
      products.value = searchResult.products || []
      // 更新库存信息
      await Promise.all(products.value.map(async product => {
        try {
          const stockRes = await getStockpile(product.id)
          if (stockRes.data.code === '200') {
            stockpiles.value[product.id] = stockRes.data.data
          }
        } catch (error) {
          console.error(`获取商品 ${product.id} 库存失败:`, error)
        }
      }))
    }
  } catch (error) {
    ElMessage.error('搜索失败')
    console.error('搜索错误:', error)
  } finally {
    isSearching.value = false
  }
}

// 添加返回方法
const handleBack = async () => {
  searchKeyword.value = ''
  await fetchProducts()
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
  await fetchAds();
  await fetchCart()
  await fetchSearchHistory()
  document.addEventListener('click', handleClickOutside)
})

// 在组件卸载时移除事件监听
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

</script>

<template>
  <ad-carousel :ads="ads"/>
  <div class="product-list-container">
    <!-- 头部 -->
    <div class="header">
      <h1>商品列表</h1>
      <div class="header-actions">
        <!-- 添加搜索框 -->
        <div class="search-container">
          <el-input
              v-model="searchKeyword"
              placeholder="搜索商品..."
              class="search-input"
              :prefix-icon="Search"
              @keyup.enter="handleSearch"
              @focus="handleSearchFocus"
          >
            <template #append>
              <el-button @click="handleSearch">搜索</el-button>
            </template>
          </el-input>
          <!-- 搜索历史 -->
          <div v-if="showHistory && searchHistory.length > 0" class="search-history">
            <div class="history-header">
              <span class="history-title">搜索历史</span>
            </div>
            <div class="history-tags">
              <el-tag
                  v-for="item in searchHistory"
                  :key="item.id"
                  class="history-tag"
                  @click.stop="searchKeyword = item.keyword; handleSearch()"
              >
                {{ item.keyword }}
              </el-tag>
            </div>
          </div>
        </div>
        <!-- 添加返回按钮 -->
        <el-button
            v-if="searchKeyword"
            type="primary"
            plain
            class="back-button"
            @click="handleBack"
        >
          返回全部商品
        </el-button>
        <el-button v-if="isAdmin" type="primary" @click="dialogVisible = true">
          新建商品
        </el-button>
      </div>
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
          <el-input v-model="form.title"/>
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

    <!-- 添加广告表单弹窗 -->
    <el-dialog
        v-model="adDialogVisible"
        title="创建广告"
        width="500px"
        @closed="currentAdProductId = ''"
    >
      <el-form :model="adForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="adForm.title" placeholder="请输入广告标题"/>
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input
              v-model="adForm.content"
              type="textarea"
              :rows="4"
              placeholder="请输入广告内容"
          />
        </el-form-item>
        <el-form-item label="广告图片" required>
          <el-upload
              :auto-upload="true"
              :http-request="handleAdImageUpload"
              :show-file-list="false"
          >
            <template #trigger>
              <el-button type="primary">上传图片</el-button>
            </template>
            <div class="cover-preview" v-if="adForm.imgUrl">
              <img
                  :src="adForm.imgUrl"
                  class="preview-image"
                  alt="广告图片预览"
              />
              <div class="preview-tip">（点击上方按钮重新上传）</div>
            </div>
            <template #tip>
              <div class="upload-tip">支持JPG/PNG格式，建议尺寸1200x400px</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createNewAd">创建</el-button>
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
    <!-- 添加编辑广告弹窗 -->
    <el-dialog
        v-model="editAdDialogVisible"
        title="编辑广告"
        width="500px"
    >
      <el-form :model="editAdForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="editAdForm.title" placeholder="请输入广告标题"/>
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input
              v-model="editAdForm.content"
              type="textarea"
              :rows="4"
              placeholder="请输入广告内容"
          />
        </el-form-item>
        <el-form-item label="广告图片" required>
          <el-upload
              :auto-upload="true"
              :http-request="handleEditAdImageUpload"
              :show-file-list="false"
          >
            <template #trigger>
              <el-button type="primary">重新上传图片</el-button>
            </template>
            <div class="cover-preview" v-if="editAdForm.imgUrl">
              <img
                  :src="editAdForm.imgUrl"
                  class="preview-image"
                  alt="广告图片预览"
              />
              <div class="preview-tip">（点击上方按钮重新上传）</div>
            </div>
            <template #tip>
              <div class="upload-tip">支持JPG/PNG格式，建议尺寸1200x400px</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editAdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="updateAd">保存修改</el-button>
      </template>
    </el-dialog>
    <!-- 商品列表 -->
    <div class="grid-container">
      <product-card
          v-for="product in products"
          :key="product.id"
          :product="product"
          :stockpile="stockpiles[product.id]"
          :is-admin="isAdmin"
          :cart-items="cartItems"
          :has-advertisement="hasAdvertisement(product.id)"
          @delete="handleDelete"
          @ad-click="handleAdClick"
          @edit-ad="openEditAdDialog"
          @stock-update="openStockDialog"
          @cart-add="(id: string) => handleCart(id, 'add')"
          @cart-subtract="(id: string) => handleCart(id, 'subtract')"
      />
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.search-container {
  position: relative;
  width: 400px;
}

.search-input {
  width: 100%;
}

.search-history {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 12px;
  margin-top: 5px;
  z-index: 1000;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.history-title {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-tag {
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 4px;
}

.history-tag:hover {
  transform: translateY(-2px);
  background-color: #ecf5ff;
  border-color: #409EFF;
  color: #409EFF;
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
  flex-direction: column;
  gap: 8px;
  background: #f8f9fa;
  padding: 12px;
  border-radius: 6px;
  position: relative;
  z-index: 2; /* 确保点击按钮时不会触发卡片点击 */
}

:deep(.el-dialog__body) {
  padding: 20px 25px;
}

.action-group {
  display: flex;
  gap: 8px;
  justify-content: space-between;
}

.action-group + .action-group {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid #eee;
}

/* 调整按钮文字间距 */
.el-button--small {
  letter-spacing: 0.5px;
  flex: 1;
  justify-content: center;
}

/* 优化hover效果 */
.el-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

/* 调整删除按钮样式 */
.el-button--danger {
  background: #ff4d4f;
  border-color: #ff4d4f;
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

.back-button {
  margin-right: 10px;
}
</style>

<script setup lang="ts">
import {ref, reactive, onMounted, computed, onUnmounted} from 'vue'
import {useRouter} from 'vue-router'
import {
  ElMessage, ElButton, ElRate, ElDialog,
  ElForm, ElFormItem, ElInput, ElInputNumber, ElLoading, ElMessageBox
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
import {Star, StarFilled, Plus, Delete, ShoppingCart, Collection} from '@element-plus/icons-vue'
import {
  BookListVO,
  getAllBookLists,
  createBookList,
  deleteBookList,
  collectBookList,
  cancelCollectBookList,
  getMyBookLists,
  getFavouriteBookLists,
  addItemToBookList,
  removeItemFromBookList,
  type BookListCreateDTO
} from '../../api/booklist'
import BookListItem from '../../components/BookListItem.vue'

import ReadingNote from '../../components/ReadingNote.vue'
import {
  createNote,
  deleteNote,
  updateNote,
  getAllNotes,
  getUserNotes,
  getPaidNotes,
  getLikedNotes,
  getNoteLikeStatus,
  likeNote,
  unlikeNote,
  payNote,
  getNotePayStatus,
  type NoteVO,
  type CreateNoteInfo
} from '../../api/note'

import FullscreenEditor from '../../components/FullscreenEditor.vue'

const showPurchaseDialog = ref(false)
const selectedNote = ref<NoteVO | null>(null)

const router = useRouter()
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

const handleCartUpdated = async () => {
  await fetchCart(); // 重新获取购物车数据
};

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
  const loading = ElLoading.service({fullscreen: false})
  try {
    const {file} = params
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
  form.specifications.push({item: '', value: '', id: '', productId: ''});
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

    if (res.data.code !== '200') {
      throw new Error(res.data.msg || '获取购物车数据失败')
    }

    const cartData = res.data.data || {}
    const items = cartData.items || []

    console.log("cartData => ", cartData)
    // 清空当前购物车数据
    cartItems.value = {}

    for (const item of items) {
      const productId = item.productId
      const currentStock = stockpiles.value[productId]?.amount || 0
      let adjustedQuantity = item.quantity

      // 检查购物车数量是否超过库存
      if (item.quantity > currentStock) {
        adjustedQuantity = currentStock
        try {
          if (adjustedQuantity > 0) {
            // 更新购物车数量到库存最大值
            await updateCartItemQuantity(item.cartItemId, adjustedQuantity)
            ElMessage.warning(`商品《${item.title}》库存不足，购物车数量已调整为最大库存数 ${adjustedQuantity}`)
          } else {
            // 删除该购物车项
            await deleteCartItem(item.cartItemId)
            ElMessage.warning(`商品《${item.title}》已无库存，已从购物车移除`)
            continue
          }
        } catch (error) {
          console.error('调整购物车失败:', error)
          ElMessage.error('自动调整购物车失败，请手动修改')
          continue
        }
      }

      // 更新本地购物车数据
      cartItems.value[productId] = {
        cartItemId: item.cartItemId,
        quantity: adjustedQuantity
      }
    }

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
    if (type === 'add' && !currentItem) {
      await addToCart(productId, 1)
      await fetchCart()
    }
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

    await adjustStockpile(currentProduct.value.id, stockForm.amount)
    const res = await getStockpile(currentProduct.value.id)

    // 更新本地库存数据
    stockpiles.value = {
      ...stockpiles.value,
      [currentProduct.value.id]: res.data.data
    }

    ElMessage.success('库存更新成功')
    stockDialogVisible.value = false

    // 重新获取购物车数据以检查数量
    await fetchCart()

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
  const loading = ElLoading.service({fullscreen: true})
  try {
    // 1. 删除关联广告
    const relatedAds = ads.value.filter(ad => ad.productId === id)
    await Promise.all(
        relatedAds.map(async ad => {
          await deleteAdvertisement(ad.id)
          // 从本地广告列表移除
          const index = ads.value.findIndex(a => a.id === ad.id)
          if (index > -1) {
            ads.value.splice(index, 1)
          }
        })
    )

    // 2. 删除商品
    await deleteProduct(id)

    // 3. 更新商品列表
    await fetchProducts()

    ElMessage.success('商品及关联广告删除成功')
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败，请重试')
  } finally {
    loading.close()
  }
}

// 书单相关状态
const activeTab = ref('products')
const booklistTab = ref('all')
const bookLists = ref<BookListVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const currentUserId = ref<number | null>(null)
const favouriteBookListIds = ref<Set<number>>(new Set())

// 创建书单相关
const createDialogVisible = ref(false)
const createForm = ref<BookListCreateDTO>({
  title: '',
  description: '',
  productIds: [],
  img: ''
})

// 书单详情相关
const detailDialogVisible = ref(false)
const currentBookList = ref<BookListVO | null>(null)

// 商品选择相关
const selectedProduct = ref<number | null>(null)

// 切换标签页
const handleTabChange = async (tab: string) => {
  activeTab.value = tab
  if (tab === 'booklists') {
    await fetchBookLists()
  } else if (tab === 'notes') {
    await fetchNotes()
  } else {
    await fetchProducts()
  }
}

// 切换书单分类
const handleBooklistTabChange = async (tab: string) => {
  booklistTab.value = tab
  currentPage.value = 1 // 切换分类时重置页码
  await fetchBookLists()
}

// 获取收藏的书单ID列表
const fetchFavouriteBookListIds = async () => {
  try {
    const res = await getFavouriteBookLists(0, 1000)
    if (res.data.data) {
      favouriteBookListIds.value = new Set(res.data.data.content.map(book => book.id))
    }
  } catch (error) {
    console.error('获取收藏书单失败:', error)
  }
}

// 获取所有书单
const fetchBookLists = async () => {
  loading.value = true
  try {
    let res
    switch (booklistTab.value) {
      case 'all':
        res = await getAllBookLists(currentPage.value - 1, pageSize.value)
        break
      case 'mine':
        res = await getMyBookLists(currentPage.value - 1, pageSize.value)
        break
      case 'favourites':
        res = await getFavouriteBookLists(currentPage.value - 1, pageSize.value)
        break
    }
    if (res && res.data.data) {
      bookLists.value = res.data.data.content
      total.value = res.data.data.total
      // 更新收藏状态
      await fetchFavouriteBookListIds()
    }
  } catch (error) {
    ElMessage.error('获取书单列表失败')
  } finally {
    loading.value = false
  }
}

// 添加商品到书单
const handleAddProduct = async (bookListId: number) => {
  if (!selectedProduct.value) {
    ElMessage.warning('请先选择要添加的商品')
    return
  }

  const loading = ElLoading.service({
    lock: true,
    text: '正在添加商品...',
    background: 'rgba(0, 0, 0, 0.7)'
  })

  try {
    await addItemToBookList(bookListId, selectedProduct.value)

    // 更新当前书单的商品列表
    if (currentBookList.value) {
      const addedProduct = products.value.find(p => p.id === selectedProduct.value)
      if (addedProduct) {
        currentBookList.value.products = [...currentBookList.value.products, addedProduct]
      }
    }

    ElMessage({
      type: 'success',
      message: '添加商品成功',
      duration: 2000
    })

    selectedProduct.value = null // 清空选择
  } catch (error) {
    ElMessage({
      type: 'error',
      message: '添加商品失败，请重试',
      duration: 2000
    })
  } finally {
    loading.close()
  }
}

// 从书单移除商品
const handleRemoveProduct = async (bookListId: number, productId: number) => {
  const loading = ElLoading.service({
    lock: true,
    text: '正在移除商品...',
    background: 'rgba(0, 0, 0, 0.7)'
  })

  try {
    await removeItemFromBookList(bookListId, productId)

    // 更新当前书单的商品列表
    if (currentBookList.value) {
      currentBookList.value.products = currentBookList.value.products.filter(p => p.id !== productId)
    }

    ElMessage({
      type: 'success',
      message: '移除商品成功',
      duration: 2000
    })
  } catch (error) {
    ElMessage({
      type: 'error',
      message: '移除商品失败，请重试',
      duration: 2000
    })
  } finally {
    loading.close()
  }
}

// 创建书单
const handleCreate = async () => {
  if (!createForm.value.title.trim()) {
    ElMessage.warning('请输入书单标题')
    return
  }

  const loading = ElLoading.service({
    lock: true,
    text: '正在创建书单...',
    background: 'rgba(0, 0, 0, 0.7)'
  })

  try {
    const res = await createBookList(createForm.value)
    ElMessage({
      type: 'success',
      message: '创建书单成功',
      duration: 2000
    })
    createDialogVisible.value = false
    createForm.value = {
      title: '',
      description: '',
      productIds: [],
      img: ''
    }
    // 如果当前在书单标签页，刷新书单列表
    if (activeTab.value === 'booklists') {
      await fetchBookLists()
    }
  } catch (error) {
    ElMessage({
      type: 'error',
      message: '创建书单失败，请重试',
      duration: 2000
    })
  } finally {
    loading.close()
  }
}

// 删除书单
const handleDeleteBookList = async (id: number) => {
  try {
    await ElMessageBox.confirm(
        '确定要删除这个书单吗？',
        '删除确认',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )

    const loading = ElLoading.service({
      lock: true,
      text: '正在删除书单...',
      background: 'rgba(0, 0, 0, 0.7)'
    })

    try {
      await deleteBookList(id)
      ElMessage({
        type: 'success',
        message: '删除书单成功',
        duration: 2000
      })
      await fetchBookLists()
    } catch (error) {
      ElMessage({
        type: 'error',
        message: '删除书单失败，请重试',
        duration: 2000
      })
    } finally {
      loading.close()
    }
  } catch {
    // 用户取消删除操作
  }
}

// 收藏/取消收藏书单
const handleCollect = async (bookList: BookListVO) => {
  try {
    const isCollected = favouriteBookListIds.value.has(bookList.id)
    if (isCollected) {
      await cancelCollectBookList({bookListId: bookList.id})
      bookList.favouriteCount--
      favouriteBookListIds.value.delete(bookList.id)
      ElMessage.success('取消收藏成功')
    } else {
      await collectBookList({bookListId: bookList.id})
      bookList.favouriteCount++
      favouriteBookListIds.value.add(bookList.id)
      ElMessage.success('收藏成功')
    }
    // 刷新当前页面的数据
    await handleTabChange(activeTab.value)
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 页码改变
const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchBookLists()
}

// 每页条数改变
const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1 // 切换每页条数时重置到第一页
  fetchBookLists()
}

// 查看书单详情
const handleViewDetail = (bookList: BookListVO) => {
  currentBookList.value = bookList
  detailDialogVisible.value = true
}

// 处理商品点击
const handleProductClick = (productId: string) => {
  router.push(`/product/${productId}`)
}

//笔记
const noteTab = ref('all')
const notes = ref<NoteVO[]>([])
const likedNoteIds = ref<Set<number>>(new Set())
const paidNoteIds = ref<Set<number>>(new Set())
const createNoteDialogVisible = ref(false)

const detailNoteDialogVisible = ref(false);
const editNoteDialogVisible = ref(false);
const currentNote = ref<NoteVO | null>(null);
const editNoteForm = reactive({
  id: -1,
  title: '',
  content: '',
  price: 0,
  img: ''
});

// 查看笔记详情
const handleViewNote = (note: NoteVO) => {
  currentNote.value = note;
  detailNoteDialogVisible.value = true;
};

// 打开编辑弹窗
const openEditNote = (note: NoteVO) => {
  detailNoteDialogVisible.value = false;
  editNoteForm.id = note.id;
  editNoteForm.title = note.title;
  editNoteForm.content = note.content;
  editNoteForm.price = note.price;
  editNoteForm.img = note.img;
  editNoteDialogVisible.value = true;
};

const noteForm = reactive<CreateNoteInfo>({
  title: '',
  content: '',
  price: 0,
  img: ''
})

// 获取笔记列表
const fetchNotes = async () => {
  try {
    let res
    switch (noteTab.value) {
      case 'all':
        res = await getAllNotes()
        break
      case 'mine':
        const userId = currentUserId.value
        if (userId) res = await getUserNotes(userId)
        break
      case 'liked':
        res = await getLikedNotes()
        break
      case 'paid':
        res = await getPaidNotes()
        break
    }
    if (res?.data?.data) {
      notes.value = res.data.data
      await checkLikeStatuses()
      await checkPayStatuses()
    }
  } catch (error) {
    ElMessage.error('获取笔记失败')
  }
}

// 处理笔记图片上传（编辑时）
const handleEditNoteImageUpload = async (params: any) => {
  const loading = ElLoading.service({ fullscreen: false });
  try {
    const { file } = params;
    const response = await uploadUserImage(file);
    editNoteForm.img = response.data.data;
    ElMessage.success('图片上传成功');
  } catch (error) {
    ElMessage.error('图片上传失败');
  } finally {
    loading.close();
  }
};

// 更新笔记
const updateNoteHandler = async () => {
  try {
    // 构造符合UpdateNoteInfo接口的对象
    const updateData = {
      id: editNoteForm.id,
      title: editNoteForm.title,
      content: editNoteForm.content,
      price: editNoteForm.price,
      img: editNoteForm.img
    };

    await updateNote(updateData);  // 传递完整的更新对象
    ElMessage.success('笔记更新成功');
    editNoteDialogVisible.value = false;
    await fetchNotes();
  } catch (error) {
    ElMessage.error('更新失败');
    console.error('更新笔记错误:', error);
  }
};

// 检查点赞状态
const checkLikeStatuses = async () => {
  for (const note of notes.value) {
    try {
      const res = await getNoteLikeStatus(note.id)
      if (res.data.data) {
        likedNoteIds.value.add(note.id)
      }
    } catch (error) {
      console.error('检查点赞状态失败:', error)
    }
  }
}

// 检查购买状态
const checkPayStatuses = async () => {
  for (const note of notes.value) {
    if (note.price <= 0) continue
    try {
      const res = await getNotePayStatus(note.id)
      if (res.data.data) {
        paidNoteIds.value.add(note.id)
      }
    } catch (error) {
      console.error('检查购买状态失败:', error)
    }
  }
}

// 处理笔记图片上传
const handleNoteImageUpload = async (params: any) => {
  const loading = ElLoading.service({fullscreen: false})
  try {
    const {file} = params
    const response = await uploadUserImage(file)
    noteForm.img = response.data.data
    ElMessage.success('图片上传成功')
  } catch (error) {
    ElMessage.error('图片上传失败')
  } finally {
    loading.close()
  }
}

// 创建新笔记
const createNewNote = async () => {
  try {
    await createNote(noteForm)
    ElMessage.success('笔记创建成功')
    createNoteDialogVisible.value = false
    Object.assign(noteForm, {
      title: '',
      content: '',
      price: 0,
      img: ''
    })
    await fetchNotes()
  } catch (error) {
    ElMessage.error('创建笔记失败')
  }
}

// 删除笔记
const handleDeleteNote = async (id: number) => {
  try {
    await deleteNote(id)
    ElMessage.success('笔记删除成功')
    notes.value = notes.value.filter(n => n.id !== id)
  } catch (error) {
    ElMessage.error('删除笔记失败')
  }
}

// 点赞/取消点赞
const handleLikeNote = async (note: NoteVO) => {
  try {
    await likeNote(note.id)
    note.likeCnt++
    likedNoteIds.value.add(note.id)
  } catch (error) {
    ElMessage.error('点赞失败')
  }
}

const handleUnlikeNote = async (note: NoteVO) => {
  try {
    await unlikeNote(note.id)
    note.likeCnt--
    likedNoteIds.value.delete(note.id)
  } catch (error) {
    ElMessage.error('取消点赞失败')
  }
}

// 购买笔记
const handlePurchaseNote = (note: NoteVO) => {
  selectedNote.value = note
  showPurchaseDialog.value = true
}

// 确认购买
const confirmPurchase = async () => {
  if (!selectedNote.value) return

  try {
    const loading = ElLoading.service({
      lock: true,
      text: '正在购买...',
      background: 'rgba(0, 0, 0, 0.7)'
    })

    try {
      const res = await payNote(selectedNote.value.id)

      // 增加业务状态码检查
      if (res.data.code !== '200') {
        throw new Error(res.data.msg || '购买失败')
      }

      ElMessage.success('购买成功')
      paidNoteIds.value.add(selectedNote.value.id)
      showPurchaseDialog.value = false
    } finally {
      loading.close()
    }
  } catch (error: any) {
    // 增强错误处理逻辑
    const errorMessage = error.response?.data?.msg ||
        error.message ||
        '购买失败，请检查网络连接或账户余额'

    // 特殊处理余额不足情况
    if (errorMessage.includes('余额不足')) {
      ElMessage.error({
        message: errorMessage,
        duration: 5000,
        showClose: true
      })
    } else {
      ElMessage.error(errorMessage)
    }
  }
}

// 全屏编辑相关状态
const fullscreenEditor = reactive({
  visible: false,
  content: '',
  mode: 'create', // 'create' | 'edit'
  title: '全屏编辑'
})

// 打开全屏编辑器
const openFullscreenEditor = (mode: 'create' | 'edit') => {
  fullscreenEditor.mode = mode
  fullscreenEditor.title = mode === 'create' ? '新建笔记-全屏编辑' : '编辑笔记-全屏编辑'
  fullscreenEditor.content = mode === 'create' ? noteForm.content : editNoteForm.content
  fullscreenEditor.visible = true
}

// 处理全屏编辑器内容更新
const handleFullscreenUpdate = (content: string) => {
  if (fullscreenEditor.mode === 'create') {
    noteForm.content = content
  } else {
    editNoteForm.content = content
  }
}

// 处理书单图片上传
const handleBookListImageUpload = async (params: any) => {
  const loading = ElLoading.service({fullscreen: false})
  try {
    const {file} = params
    const response = await uploadUserImage(file)
    createForm.value.img = response.data.data
    ElMessage.success('图片上传成功')
  } catch (error) {
    ElMessage.error('图片上传失败')
  } finally {
    loading.close()
  }
}

onMounted(async () => {
  // 先获取用户信息
  try {
    const username = sessionStorage.getItem('username')
    if (username) {
      const res = await getUserInfo(username)
      isAdmin.value = res.data.data?.role === 'admin'
      currentUserId.value = res.data.data?.id
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户权限失败')
  }

  // 然后获取商品列表
  await fetchProducts()
  await fetchAds();
  await fetchCart()
  if (activeTab.value === 'notes') {
    await fetchNotes()
  }
})

// 在组件卸载时移除事件监听
onUnmounted(() => {
  // 删除事件监听
  // document.removeEventListener('click', handleClickOutside)
})

</script>

<template>
  <ad-carousel :ads="ads"/>
  <div class="product-list-container">
    <!-- 导航栏 -->
    <div class="nav-tabs">
      <el-button
          :type="activeTab === 'products' ? 'primary' : 'default'"
          @click="handleTabChange('products')"
      >
        商品列表
      </el-button>
      <el-button
          :type="activeTab === 'booklists' ? 'primary' : 'default'"
          @click="handleTabChange('booklists')"
      >
        书单列表
      </el-button>
      <el-button
          :type="activeTab === 'notes' ? 'primary' : 'default'"
          @click="handleTabChange('notes')"
      >
        读书笔记
      </el-button>
    </div>

    <!-- 笔记详情弹窗 -->
    <el-dialog
        v-model="detailNoteDialogVisible"
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
            style="max-width: 100%; margin: 10px 0;"
        />
        <div class="note-content" style="white-space: pre-wrap;">{{ currentNote.content }}</div>
        <div class="actions" v-if="currentUserId === currentNote.creatorId" style="margin-top: 20px;">
          <el-button type="primary" @click="openEditNote(currentNote)">编辑笔记</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 编辑笔记弹窗 -->
    <el-dialog
        v-model="editNoteDialogVisible"
        title="编辑笔记"
        width="600px"
    >
      <el-form :model="editNoteForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="editNoteForm.title" />
        </el-form-item>
        <el-form-item label="内容" required>
          <div class="content-editor">
            <el-input
                v-model="editNoteForm.content"
                type="textarea"
                :rows="4"
                resize="none"
            />
            <el-button
                type="primary"
                plain
                @click="openFullscreenEditor('edit')"
                class="fullscreen-btn"
            >
              全屏模式
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number
              v-model="editNoteForm.price"
              :min="0"
              :precision="2"
          />
        </el-form-item>
        <el-form-item label="封面图">
          <el-upload
              :auto-upload="true"
              :http-request="handleEditNoteImageUpload"
              :show-file-list="false"
          >
            <template #trigger>
              <el-button type="primary">上传新图片</el-button>
            </template>
            <img
                v-if="editNoteForm.img"
                :src="editNoteForm.img"
                class="preview-image"
                style="max-width: 200px; margin-top: 10px;"
            />
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editNoteDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="updateNoteHandler">保存修改</el-button>
      </template>
    </el-dialog>

    <!-- 新建笔记对话框 -->
    <el-dialog
        v-model="createNoteDialogVisible"
        title="新建笔记"
        width="600px"
    >
      <el-form :model="noteForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="noteForm.title"/>
        </el-form-item>
        <el-form-item label="内容" required>
          <div class="content-editor">
            <el-input
                v-model="noteForm.content"
                type="textarea"
                :rows="4"
                resize="none"
            />
            <el-button
                type="primary"
                plain
                @click="openFullscreenEditor('create')"
                class="fullscreen-btn"
            >
              全屏模式
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="价格 🍅">
          <el-input-number
              v-model="noteForm.price"
              :min="0"
              :precision="0"
          />
        </el-form-item>
        <el-form-item label="封面图">
          <el-upload
              :auto-upload="true"
              :http-request="handleNoteImageUpload"
              :show-file-list="false"
          >
            <template #trigger>
              <el-button type="primary">上传图片</el-button>
            </template>
            <img
                v-if="noteForm.img"
                :src="noteForm.img"
                class="preview-image"
                style="max-width: 200px; margin-top: 10px;"
            />
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createNoteDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createNewNote">创建</el-button>
      </template>
    </el-dialog>

    <!-- 头部 -->
    <div class="header">
      <h1>        {{
          activeTab === 'products'
              ? '商品列表'
              : activeTab === 'booklists'
                  ? '书单列表'
                  : '读书笔记'
        }}</h1>
      <div class="header-actions">
        <!-- 删除搜索框相关代码 -->
        <el-button v-if="isAdmin && activeTab === 'products'" type="primary" @click="dialogVisible = true">
          新建商品
        </el-button>
        <el-button v-if="activeTab === 'booklists'" type="primary" @click="createDialogVisible = true">
          <el-icon>
            <Plus/>
          </el-icon>
          创建书单
        </el-button>
        <el-button
            v-if="activeTab === 'notes'"
            type="primary"
            @click="createNoteDialogVisible = true"
        >
          <el-icon><Plus /></el-icon>
          发布笔记
        </el-button>
      </div>
    </div>

    <!-- 商品列表 -->
    <div v-if="activeTab === 'products'" class="grid-container">
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
          @cart-updated="handleCartUpdated"
      />
    </div>

    <!-- 书单列表 -->
    <div v-else-if="activeTab === 'booklists'">
      <!-- 书单子导航栏 -->
      <div class="booklist-sub-tabs">
        <el-button
            :type="booklistTab === 'all' ? 'primary' : 'default'"
            @click="handleBooklistTabChange('all')"
        >
          全部书单
        </el-button>
        <el-button
            :type="booklistTab === 'mine' ? 'primary' : 'default'"
            @click="handleBooklistTabChange('mine')"
        >
          我的书单
        </el-button>
        <el-button
            :type="booklistTab === 'favourites' ? 'primary' : 'default'"
            @click="handleBooklistTabChange('favourites')"
        >
          收藏的书单
        </el-button>
      </div>

      <div class="booklist-grid" v-loading="loading">
        <book-list-item
            v-for="bookList in bookLists"
            :key="bookList.id"
            :book-list="bookList"
            :is-favourite="favouriteBookListIds.has(bookList.id)"
            :is-creator="currentUserId === bookList.creatorId"
            @collect="handleCollect"
            @delete="handleDeleteBookList"
            @view="handleViewDetail"
        />
      </div>
    </div>

    <!-- 读书笔记部分 -->
    <div v-else-if="activeTab === 'notes'">
      <!-- 子导航栏 -->
      <div class="note-sub-tabs">
        <el-button
            :type="noteTab === 'all' ? 'primary' : 'default'"
            @click="noteTab = 'all'; fetchNotes()"
        >
          所有笔记
        </el-button>
        <el-button
            :type="noteTab === 'mine' ? 'primary' : 'default'"
            @click="noteTab = 'mine'; fetchNotes()"
        >
          我的笔记
        </el-button>
        <el-button
            :type="noteTab === 'liked' ? 'primary' : 'default'"
            @click="noteTab = 'liked'; fetchNotes()"
        >
          赞过的笔记
        </el-button>
        <el-button
            :type="noteTab === 'paid' ? 'primary' : 'default'"
            @click="noteTab = 'paid'; fetchNotes()"
        >
          购买的笔记
        </el-button>
      </div>

      <div class="note-grid">
        <ReadingNote
            v-for="note in notes"
            :key="note.id"
            :note="note"
            :is-liked="likedNoteIds.has(note.id)"
            :is-creator="currentUserId === note.creatorId"
            :is-paid="paidNoteIds.has(note.id)"
            @like="handleLikeNote"
            @unlike="handleUnlikeNote"
            @delete="handleDeleteNote"
            @purchase="handlePurchaseNote"
            @view="handleViewNote"
        />
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="total > 0 && activeTab === 'booklists'">
      <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
      />
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

    <!-- 创建书单对话框 -->
    <el-dialog
        v-model="createDialogVisible"
        title="创建书单"
        width="500px"
    >
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="createForm.title" placeholder="请输入书单标题"/>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
              v-model="createForm.description"
              type="textarea"
              :rows="4"
              placeholder="请输入书单描述"
          />
        </el-form-item>
        <el-form-item label="封面图">
          <el-upload
              :auto-upload="true"
              :http-request="handleBookListImageUpload"
              :show-file-list="false"
          >
            <template #trigger>
              <el-button type="primary">上传图片</el-button>
            </template>
            <div class="cover-preview" v-if="createForm.img">
              <img
                  :src="createForm.img"
                  class="preview-image"
                  alt="书单封面预览"
              />
              <div class="preview-tip">（点击上方按钮重新上传）</div>
            </div>
            <template #tip>
              <div class="upload-tip">支持JPG/PNG格式，建议尺寸800x800px</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="商品">
          <el-select
              v-model="createForm.productIds"
              multiple
              filterable
              placeholder="请选择商品"
              style="width: 100%"
          >
            <el-option
                v-for="product in products"
                :key="product.id"
                :label="product.title"
                :value="product.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 书单详情对话框 -->
    <el-dialog
        v-model="detailDialogVisible"
        title="书单详情"
        width="800px"
    >
      <div v-if="currentBookList" class="booklist-detail">
        <h2>{{ currentBookList.title }}</h2>
        <p class="description">{{ currentBookList.description }}</p>

        <div class="products-list">
          <div
              v-for="product in currentBookList.products"
              :key="product.id"
              class="product-item"
              @click="handleProductClick(product.id)"
          >
            <img :src="product.cover" :alt="product.title" class="product-cover">
            <div class="product-info">
              <h4>{{ product.title }}</h4>
              <p class="price">¥{{ product.price }}</p>
            </div>
            <div class="product-actions">
              <el-button
                  v-if="currentUserId === currentBookList.creatorId"
                  type="danger"
                  circle
                  @click.stop="handleRemoveProduct(currentBookList.id, product.id)"
              >
                <el-icon>
                  <Delete/>
                </el-icon>
              </el-button>
            </div>
          </div>
        </div>

        <div v-if="currentUserId === currentBookList.creatorId" class="add-product">
          <el-select
              v-model="selectedProduct"
              filterable
              placeholder="添加商品到书单"
              style="width: 100%"
          >
            <el-option
                v-for="product in products"
                :key="product.id"
                :label="product.title"
                :value="product.id"
            />
          </el-select>
          <el-button
              type="primary"
              @click="handleAddProduct(currentBookList.id)"
              :disabled="!selectedProduct"
          >
            添加商品
          </el-button>
        </div>
      </div>
    </el-dialog>
    <!-- 购买确认弹窗 -->
    <el-dialog
        v-model="showPurchaseDialog"
        title="确认购买"
        width="500px"
        class="purchase-confirm-dialog"
    >
      <div v-if="selectedNote" class="confirm-purchase">
        <img
            :src="selectedNote.img || '/default-note-cover.png'"
            class="note-cover"
            alt="笔记封面"
        />
        <div class="content">
          <h3>是否确认购买《{{ selectedNote.title }}》？</h3>
          <p class="price">价格：{{ selectedNote.price }} 🍅</p>
        </div>
      </div>

      <template #footer>
        <el-button @click="showPurchaseDialog = false">取消</el-button>
        <el-button
            type="primary"
            @click="confirmPurchase"
            :loading="confirmLoading"
        >
          确认购买
        </el-button>
      </template>
    </el-dialog>
  </div>
  <FullscreenEditor
      v-model="fullscreenEditor.visible"
      v-model:content="fullscreenEditor.content"
      :title="fullscreenEditor.title"
      @update:content="handleFullscreenUpdate"
  />
</template>

<style scoped>
.product-list-container {
  padding: 24px;
  background: linear-gradient(120deg, #e3f6f5 0%, #d0eeff 100%);
  min-height: 100vh;
}

.nav-tabs {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-bottom: 32px;
  padding: 0;
  border-bottom: 1px solid #ebeef5;
}

.nav-tabs .el-button {
  border: none;
  background: none;
  font-size: 16px;
  padding: 12px 0;
  position: relative;
  color: #606266;
  transition: all 0.3s;
  font-weight: 400;
}

.nav-tabs .el-button:hover {
  color: #2c698d;
  transform: none;
  box-shadow: none;
}

.nav-tabs .el-button.is-primary {
  color: #1a4b6e;
  background: none;
  border: none;
  font-weight: 600;
}

.nav-tabs .el-button.is-primary::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 3px;
  background-color: #1a4b6e;
  transition: all 0.3s;
  border-radius: 2px 2px 0 0;
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

.booklist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

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

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 20px;
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.pagination :deep(.el-pagination) {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 16px;
  border-radius: 8px;
  background: #f8f9fa;
}

.pagination :deep(.el-pagination .el-pagination__total) {
  margin-right: 16px;
  font-weight: 500;
  color: #606266;
}

.pagination :deep(.el-pagination .el-pagination__sizes) {
  margin-right: 16px;
}

.pagination :deep(.el-pagination .el-select .el-input) {
  width: 110px;
}

.pagination :deep(.el-pagination .el-pagination__sizes .el-input__inner) {
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  background-color: white;
}

.pagination :deep(.el-pagination .btn-prev),
.pagination :deep(.el-pagination .btn-next) {
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 0 12px;
  height: 32px;
  line-height: 32px;
  transition: all 0.3s;
}

.pagination :deep(.el-pagination .btn-prev:hover),
.pagination :deep(.el-pagination .btn-next:hover) {
  color: #409EFF;
  border-color: #409EFF;
  background-color: #ecf5ff;
}

.pagination :deep(.el-pagination .el-pager li) {
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  transition: all 0.3s;
}

.pagination :deep(.el-pagination .el-pager li:hover) {
  color: #409EFF;
  border-color: #409EFF;
  background-color: #ecf5ff;
}

.pagination :deep(.el-pagination .el-pager li.active) {
  background-color: #409EFF;
  color: white;
  border-color: #409EFF;
  font-weight: bold;
}

.pagination :deep(.el-pagination .el-pager li.active:hover) {
  background-color: #66b1ff;
  border-color: #66b1ff;
}

.clickable {
  cursor: pointer;
}

.clickable:hover {
  color: #409EFF;
}

.booklist-detail {
  padding: 20px;
}

.products-list {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.product-item {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.product-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.product-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 2;
}

.add-product {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}

.booklist-sub-tabs {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-bottom: 32px;
  padding: 0;
  border-bottom: 1px solid #ebeef5;
}

.booklist-sub-tabs .el-button {
  border: none;
  background: none;
  font-size: 16px;
  padding: 12px 0;
  position: relative;
  color: #606266;
  transition: all 0.3s;
  font-weight: 400;
}

.booklist-sub-tabs .el-button:hover {
  color: #2c698d;
  transform: none;
  box-shadow: none;
}

.booklist-sub-tabs .el-button.is-primary {
  color: #1a4b6e;
  background: none;
  border: none;
  font-weight: 600;
}

.booklist-sub-tabs .el-button.is-primary::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 3px;
  background-color: #1a4b6e;
  transition: all 0.3s;
  border-radius: 2px 2px 0 0;
}

.note-sub-tabs {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 24px;
}

.note-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  padding: 20px;
}


.note-sub-tabs {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 32px;
  padding: 0;
  border-bottom: 1px solid #ebeef5;
}

.note-sub-tabs .el-button {
  border: none;
  background: none;
  font-size: 16px;
  padding: 12px 0;
  position: relative;
  color: #606266;
  transition: all 0.3s;
  font-weight: 400;
}

.note-sub-tabs .el-button:hover {
  color: #2c698d;
  transform: none;
  box-shadow: none;
}

.note-sub-tabs .el-button.is-primary {
  color: #1a4b6e;
  background: none;
  border: none;
  font-weight: 600;
}

.note-sub-tabs .el-button.is-primary::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 3px;
  background-color: #1a4b6e;
  transition: all 0.3s;
  border-radius: 2px 2px 0 0;
}

/* 统一新建按钮样式 */
.new-note-btn {
  height: 200px;
  border: 2px dashed #dcdfe6;
  background-color: #f8f9fa;
  color: #606266;
  transition: all 0.3s;
}

.new-note-btn:hover {
  border-color: #2c698d;
  color: #2c698d;
  transform: translateY(-3px);
}
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.detail-header h2 {
  font-size: 24px;
  color: #303133;
  margin: 0;
}

.detail-price {
  font-size: 18px;
  color: #e6a23c;
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-price .paid {
  color: #67c23a;
}

.detail-price .paid-badge {
  background: #f0f9eb;
  color: #67c23a;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 14px;
}

.detail-price .free {
  color: #909399;
}

.confirm-purchase {
  text-align: center;

  .note-cover {
    max-width: 200px;
    max-height: 150px;
    border-radius: 4px;
    margin: 10px 0;
  }

  .price {
    color: #e6a23c;
    font-weight: bold;
    margin: 8px 0;
  }
}

.confirm-purchase {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 12px;

  .note-cover {
    width: 200px;
    height: 150px;
    object-fit: cover;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  .content {
    text-align: center;

    h3 {
      margin: 0 0 12px 0;
      color: #303133;
      font-size: 16px;
    }

    .price {
      color: #e6a23c;
      font-weight: bold;
      font-size: 14px;
    }
  }
}

:deep(.purchase-confirm-dialog) {
  .el-dialog__header {
    border-bottom: 1px solid #ebeef5;
  }

  .el-dialog__footer {
    border-top: 1px solid #ebeef5;
    padding: 16px 20px;
  }
}
</style>
<!-- Dashboard.vue -->
<script setup lang="ts">
import {ref, onMounted, computed, reactive} from 'vue'
import {useRouter} from 'vue-router'
import {getUserInfo, updateUserInfo} from '../../api/user'
import {ElMessage, ElLoading, ElDialog, ElMessageBox, type FormInstance} from 'element-plus'
import {UserFilled, Food, Money, Delete} from '@element-plus/icons-vue'
import {uploadUserImage} from '../../api/util'
import {getProducts, type Product} from '../../api/product'
import {submitTomatoRecharge, payOrder, alipayHelper, getAllOrders, type OrderVO} from '../../api/order'
import BookListItem from '../../components/BookListItem.vue'
import {applyVerification, type ApplyVerificationRequest} from '../../api/verification'
import {ElTag} from 'element-plus'
import axios from "axios";
import {
  getUserNotes,
  getPaidNotes,
  getLikedNotes,
  deleteNote,
  likeNote,
  unlikeNote,
  payNote,
  updateNote,
  type NoteVO
} from '../../api/note'
import {getMyVerifications, type VerificationVO} from '../../api/verification'
import UserBadge from '../../components/UserBadge.vue'
import ReadingNote from "../../components/ReadingNote.vue";
import {
  addItemToBookList,
  BookListVO,
  cancelCollectBookList,
  collectBookList, deleteBookList,
  getAllBookLists,
  getFavouriteBookLists, removeItemFromBookList
} from "../../api/booklist.ts";


interface UploadFile {
  url: string
  name?: string
  status?: string
}

const proofFiles = ref<UploadFile[]>([])
const router = useRouter()
const userData = ref({
  username: '',
  name: '',
  avatar: '',
  telephone: '',
  email: '',
  location: '',
  role: '',
  password: '',
  confirmPassword: '',
  isVerified: false,
  followingCount: 0,
  followerCount: 0,
  tomato: 0,
  verifiedName: ''
})
const originalPassword = ref('')
const showReloginDialog = ref(false)
const editMode = ref(false)
const tempAvatar = ref('')
const formRef = ref<FormInstance>()
const isChangingPassword = ref(false)
const role = ref('')

// 添加书单相关的状态
const activeTab = ref('created')
const createdBookLists = ref<BookListVO[]>([])
const favouriteBookLists = ref<BookListVO[]>([])
const loading = ref(false)
const favouriteBookListIds = ref<Set<number>>(new Set())
const currentUserId = ref<number | null>(null)

// 书单详情相关
const detailDialogVisible = ref(false)
const currentBookList = ref<BookListVO | null>(null)
const selectedProduct = ref<number | null>(null)
const products = ref<Product[]>([])

//笔记相关状态
const activeNoteTab = ref('created-notes')
const createdNotes = ref<NoteVO[]>([])
const paidNotes = ref<NoteVO[]>([])
const likedNotes = ref<NoteVO[]>([])
const likedNoteIds = ref<number[]>([])
const paidNoteIds = ref<Set<number>>(new Set())
const loadingNotes = ref(false)
const detailNoteDialogVisible = ref(false)
const currentNote = ref<NoteVO | null>(null)
const editNoteDialogVisible = ref(false)
const editNoteForm = reactive({
  id: -1,
  title: '',
  content: '',
  price: 0,
  img: ''
})

// 圣女果充值相关
const showRechargeDialog = ref(false)
const rechargeAmount = ref(0)
const rechargeFormRef = ref<FormInstance>()
const rechargeRules = {
  amount: [
    {required: true, message: '请输入充值数量', trigger: 'blur'},
    {type: 'number', min: 1, message: '充值数量必须大于0', trigger: 'blur'}
  ]
}

const rules = {
  username: [
    {required: true, message: '请输入用户名', trigger: 'blur'},
    {
      pattern: /^[a-zA-Z0-9]{4,20}$/,
      message: '4-20位字母数字组合',
      trigger: 'blur'
    }
  ],
  name: [
    {required: true, message: '请输入真实姓名', trigger: 'blur'},
    {min: 2, max: 10, message: '长度在2-10个字符', trigger: 'blur'}
  ],
  telephone: [
    {
      pattern: /^1(3[0-9]|4[579]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[189])\d{8}$/,
      message: '请输入有效的手机号码',
      trigger: 'blur'
    }
  ],
  email: [
    {
      type: 'email',
      pattern: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
      message: '请输入有效的邮箱地址',
      trigger: ['blur', 'change']
    }
  ],
  password: [
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (!isChangingPassword.value) return callback()
        if (!value) return callback(new Error('请输入密码'))
        if (value.length < 6 || value.length > 20) {
          return callback(new Error('长度在6-20个字符'))
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (!isChangingPassword.value) return callback()
        if (!value) return callback(new Error('请确认密码'))
        if (value !== userData.value.password) {
          return callback(new Error('两次输入密码不一致'))
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

//大V认证
// 新增认证申请对话框相关状态
const applyDialogVisible = ref(false)
const applyForm = ref({
  reasonText: '',
  proofImgs: [] as string[],
  verifiedName: ''
})

const applyRules = {
  reasonText: [
    {required: true, message: '请输入申请理由', trigger: 'blur'},
    {min: 20, message: '申请理由至少20字', trigger: 'blur'}
  ],
  verifiedName: [
    {required: true, message: '请选择认证名号', trigger: 'change'}
  ]
}

const recordDialogVisible = ref(false)
const verificationRecords = ref<VerificationVO[]>([])
const currentPage = ref(1)
const pageSize = ref(5)
const totalRecords = ref(0)
const loadingRecords = ref(false)
const orders = ref<OrderVO[]>([])
const loadingOrders = ref(false)


const fetchUserInfo = async () => {
  const username = sessionStorage.getItem('username')
  if (!username) {
    ElMessage.error('未获取到用户信息，请重新登录')
    return
  }

  try {
    const res = await getUserInfo(username)
    originalPassword.value = res.data.data.password
    userData.value = {
      username: res.data.data.username,
      name: res.data.data.name,
      avatar: res.data.data.avatar,
      telephone: res.data.data.telephone || '',
      email: res.data.data.email || '',
      location: res.data.data.location || '',
      role: res.data.data.role,
      password: '',
      confirmPassword: '',
      isVerified: res.data.data.isVerified,
      followingCount: res.data.data.followingCount,
      followerCount: res.data.data.followerCount,
      tomato: res.data.data.tomato || 0,
      verifiedName: res.data.data.verifiedName || ''
    }
    sessionStorage.setItem('role', userData.value.role);
    role.value = userData.value.role === 'user' ? "顾客" : "管理员";
    tempAvatar.value = res.data.data.avatar || ''

  } catch (error) {
    ElMessage.error('获取用户信息失败')
  }
}

// 新增认证名号选项
const verifiedNameOptions = [
  '墨香雅士',
  '当代鲁迅',
  '读书达人',
  '藏书阁主',
  '笔记大师'
]

const fetchVerificationRecords = async () => {
  loadingRecords.value = true
  try {
    const res = await getMyVerifications(currentPage.value - 1, pageSize.value)
    console.log("get my v => ", res)
    if (res.data.code === '200') {
      verificationRecords.value = res.data.data.content
      totalRecords.value = res.data.data.total
    }
  } catch (error) {
    ElMessage.error('获取申请记录失败')
  } finally {
    loadingRecords.value = false
  }
}

// 处理分页变化
const handlePageChange = (newPage: number) => {
  currentPage.value = newPage
  fetchVerificationRecords()
}

// 处理证明材料上传
const handleProofUpload = async (params: any) => {
  const loading = ElLoading.service({fullscreen: false})
  try {
    const {file} = params
    const response = await uploadUserImage(file)
    applyForm.value.proofImgs.push(response.data.data)
    proofFiles.value = applyForm.value.proofImgs.map(url => ({url}))
    ElMessage.success('上传成功')
  } catch (error) {
    ElMessage.error('证明材料上传失败')
  } finally {
    loading.close()
  }
}

// 处理文件移除
const handleProofRemove = (file: UploadFile) => {
  const index = applyForm.value.proofImgs.indexOf(file.url)
  if (index !== -1) {
    applyForm.value.proofImgs.splice(index, 1)
  }
  proofFiles.value = proofFiles.value.filter(f => f.url !== file.url)
}

// 上传前校验
const beforeProofUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过5MB')
    return false
  }
  return true
}

// 新增认证申请方法
const handleApplyVerification = async () => {
  try {
    // 验证表单
    await formRef.value?.validate()

    ElLoading.service()
    const request: ApplyVerificationRequest = {
      reasonText: applyForm.value.reasonText,
      proofImgs: applyForm.value.proofImgs,
      verifiedName: applyForm.value.verifiedName
    }

    const res = await applyVerification(request)
    if (res.data.code == '400') {
      ElMessage.error('申请提交失败！已有待审核申请')
      return
    }
    await fetchUserInfo()
    ElMessage.success('申请已提交，请等待审核')
    applyDialogVisible.value = false
    applyForm.value = {reasonText: '', proofImgs: [], verifiedName: ''}
  } catch (error) {
    if (axios.isAxiosError(error)) {
      ElMessage.error(error.response?.data?.msg || '提交申请失败')
    } else {
      ElMessage.error('提交申请失败')
    }
  } finally {
    ElLoading.service().close()
  }
}

const handleSubmit = async () => {
  try {
    if (isChangingPassword.value) {
      await formRef.value?.validateField(['password', 'confirmPassword'])
    }
    await formRef.value?.validate()

    const updateData = {
      username: userData.value.username,
      name: userData.value.name || undefined,
      avatar: tempAvatar.value || undefined,
      telephone: userData.value.telephone || undefined,
      email: userData.value.email || undefined,
      location: userData.value.location || undefined,
      role: userData.value.role || undefined,
      password: isChangingPassword.value ? userData.value.password : undefined
    }

    await updateUserInfo(updateData)
    ElMessage.success('信息更新成功')

    if (isChangingPassword.value) {
      originalPassword.value = userData.value.password
      showReloginDialog.value = true
    } else {
      editMode.value = false
      await fetchUserInfo()
    }
  } catch (error) {
    ElMessage.error('表单验证失败，请检查输入')
  }
}

const togglePasswordChange = () => {
  isChangingPassword.value = !isChangingPassword.value
  if (!isChangingPassword.value) {
    userData.value.password = ''
    userData.value.confirmPassword = ''
    formRef.value?.clearValidate(['password', 'confirmPassword'])
  }
}

const cancelEdit = () => {
  editMode.value = false
  isChangingPassword.value = false
  userData.value.password = ''
  userData.value.confirmPassword = ''
  fetchUserInfo()
}

//读书笔记相关
// 获取创建的笔记
const fetchCreatedNotes = async () => {
  try {
    const res = await getUserNotes(currentUserId.value!)
    createdNotes.value = res.data.data
  } catch (error) {
    ElMessage.error('获取创建的笔记失败')
  }
}

// 获取购买的笔记
const fetchPaidNotes = async () => {
  try {
    const res = await getPaidNotes()
    paidNotes.value = res.data.data
    paidNoteIds.value = new Set(paidNotes.value.map(n => n.id))
  } catch (error) {
    ElMessage.error('获取购买的笔记失败')
  }
}

// 获取收藏的笔记
const fetchLikedNotes = async () => {
  try {
    const res = await getLikedNotes()
    likedNotes.value = res.data.data
    likedNoteIds.value = likedNotes.value.map(n => n.id) // 改为数组
  } catch (error) {
    ElMessage.error('获取收藏的笔记失败')
  }
}

const handleViewNote = (note: NoteVO) => {
  currentNote.value = note
  detailNoteDialogVisible.value = true
}

const handleEditNote = (note: NoteVO) => {
  editNoteForm.id = note.id
  editNoteForm.title = note.title
  editNoteForm.content = note.content
  editNoteForm.price = note.price
  editNoteForm.img = note.img
  editNoteDialogVisible.value = true
  detailNoteDialogVisible.value = false
}

const getDisplayContent = (content: string, isPaid: boolean) => {
  if (isPaid || !content) return content
  const showLength = Math.ceil(content.length * 0.35)
  return content.slice(0, showLength) + '...'
}

const updateNoteHandler = async () => {
  try {
    await updateNote({
      id: editNoteForm.id,
      title: editNoteForm.title,
      content: editNoteForm.content,
      price: editNoteForm.price,
      img: editNoteForm.img
    })

    // 更新本地数据
    const notesArray = activeNoteTab.value === 'created-notes' ? createdNotes.value :
        activeNoteTab.value === 'paid-notes' ? paidNotes.value :
            likedNotes.value;
    const index = notesArray.findIndex(n => n.id === editNoteForm.id)
    if (index > -1) {
      notesArray[index] = {
        ...notesArray[index],
        ...editNoteForm
      }
    }

    ElMessage.success('笔记更新成功')
    editNoteDialogVisible.value = false
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

const handleEditNoteImageUpload = async (params: any) => {
  const loading = ElLoading.service({fullscreen: false})
  try {
    const {file} = params
    const response = await uploadUserImage(file)
    editNoteForm.img = response.data.data
    ElMessage.success('图片上传成功')
  } catch (error) {
    ElMessage.error('图片上传失败')
  } finally {
    loading.close()
  }
}

// 初始化获取笔记数据
const fetchAllNotes = async () => {
  loadingNotes.value = true
  try {
    await Promise.all([
      fetchCreatedNotes(),
      fetchPaidNotes(),
      fetchLikedNotes()
    ])
  } finally {
    loadingNotes.value = false
  }
}

// 处理笔记点赞
const handleLikeNote = async (note: NoteVO) => {
  try {
    await likeNote(note.id)
    likedNoteIds.value = [...likedNoteIds.value, note.id] // 创建新数组
    note.likeCnt++
    fetchLikedNotes()
    ElMessage.success('点赞成功')
  } catch (error) {
    ElMessage.error('点赞失败')
  }
}

const handleUnlikeNote = async (note: NoteVO) => {
  try {
    await unlikeNote(note.id)
    likedNoteIds.value = likedNoteIds.value.filter(id => id !== note.id) // 过滤数组
    note.likeCnt--
    fetchLikedNotes()
    ElMessage.success('取消点赞成功')
  } catch (error) {
    ElMessage.error('取消点赞失败')
  }
}

// 处理删除笔记
const handleDeleteNote = async (id: number) => {
  try {
    await deleteNote(id)
    createdNotes.value = createdNotes.value.filter(n => n.id !== id)
    fetchPaidNotes()
    ElMessage.success('删除成功')
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 处理购买笔记
const handlePurchaseNote = async (note: NoteVO) => {
  try {
    await payNote(note.id)
    paidNoteIds.value.add(note.id)
    await fetchPaidNotes()
    fetchPaidNotes()
    ElMessage.success('购买成功')
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '购买失败')
  }
}

// 获取用户创建的书单
const fetchCreatedBookLists = async () => {
  try {
    const res = await getAllBookLists()
    if (res.data.code === '200') {
      createdBookLists.value = res.data.data.content.filter(
          (list: BookListVO) => list.creatorId === Number(sessionStorage.getItem('userId'))
      )
    }
  } catch (error) {
    ElMessage.error('获取创建的书单失败')
  }
}

// 获取用户收藏的书单
const fetchFavouriteBookLists = async () => {
  try {
    const res = await getFavouriteBookLists()
    if (res.data.code === '200') {
      favouriteBookLists.value = res.data.data.content
      // 更新收藏状态
      favouriteBookListIds.value = new Set(favouriteBookLists.value.map(book => book.id))
    }
  } catch (error) {
    ElMessage.error('获取收藏的书单失败')
  }
}

// 处理收藏/取消收藏
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
    // 刷新书单列表
    if (activeTab.value === 'favourite') {
      await fetchFavouriteBookLists()
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 处理删除书单
const handleDelete = async (id: number) => {
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
      ElMessage.success('删除书单成功')
      await fetchCreatedBookLists()
    } catch (error) {
      ElMessage.error('删除书单失败，请重试')
    } finally {
      loading.close()
    }
  } catch {
    // 用户取消删除操作
  }
}

// 处理查看书单详情
const handleView = (bookList: BookListVO) => {
  currentBookList.value = bookList
  detailDialogVisible.value = true
}

// 处理商品点击
const handleProductClick = (productId: string) => {
  router.push(`/product/${productId}`)
}

onMounted(() => {
  fetchUserInfo()
  currentUserId.value = Number(sessionStorage.getItem('userId'))
  fetchCreatedBookLists()
  fetchFavouriteBookLists()
  fetchOrders()
  fetchProducts()
  fetchAllNotes()
})

const handleRelogin = () => {
  sessionStorage.clear()
  router.push('/login')
}

const handleAvatarUpload = async (params: any) => {
  const loading = ElLoading.service({fullscreen: false});
  try {
    const {file} = params;
    const response = await uploadUserImage(file);
    tempAvatar.value = response.data.data;
    const updateData = {
      username: userData.value.username,
      name: userData.value.name || undefined,
      avatar: tempAvatar.value || undefined,
      telephone: userData.value.telephone || undefined,
      email: userData.value.email || undefined,
      location: userData.value.location || undefined,
      role: userData.value.role || undefined
    }
    await updateUserInfo(updateData)
    ElMessage.success('头像上传成功');
  } catch (error) {
    ElMessage.error('头像上传失败，请重试');
  } finally {
    loading.close();
  }
};

// 处理圣女果充值
const handleRecharge = async () => {
  if (!rechargeFormRef.value) return

  try {
    await rechargeFormRef.value.validate()

    const loading = ElLoading.service({
      lock: true,
      text: '正在创建充值订单...',
      background: 'rgba(0, 0, 0, 0.7)'
    })

    try {
      // 计算实际支付金额（1元=10个圣女果）
      const actualAmount = rechargeAmount.value / 10;

      // 提交充值订单
      const rechargeData = {
        tomato: rechargeAmount.value,
        payment_method: 'ALIPAY',
        shipping_address: {
          recipientName: "系统充值",
          telephone: "00000000000",
          zipCode: "000000",
          location: "系统充值"
        }
      };

      console.log('=== 充值调试信息 ===');
      console.log('1. 充值请求数据:', rechargeData);
      console.log('   实际支付金额:', actualAmount, '元');

      const response = await submitTomatoRecharge(rechargeData)
      console.log('2. 充值订单响应:', response);

      if (response.data.code === '200') {
        console.log('3. 订单创建成功，准备调用支付接口');
        // 调用支付接口
        const payResponse = await payOrder(response.data.data.orderId)
        console.log('4. 支付接口响应:', payResponse);

        // 渲染支付表单
        console.log('5. 准备渲染支付表单');
        alipayHelper.renderPaymentForm(payResponse.paymentForm)

        showRechargeDialog.value = false
        rechargeAmount.value = 0
        console.log('6. 充值流程完成，等待支付结果');
      } else {
        console.error('充值订单创建失败:', response.data.msg);
        throw new Error(response.data.msg || '创建充值订单失败')
      }
    } finally {
      loading.close()
    }
  } catch (error) {
    console.error('充值失败:', error)
    ElMessage.error('充值失败，请重试')
  }
}

// 修改充值对话框中的金额显示
const calculateTotalAmount = computed(() => {
  return (rechargeAmount.value / 10).toFixed(2);
});

// 添加获取订单的方法
const fetchOrders = async () => {
  loadingOrders.value = true
  try {
    const response = await getAllOrders()
    orders.value = response
  } catch (error) {
    ElMessage.error('获取订单历史失败')
  } finally {
    loadingOrders.value = false
  }
}

// 格式化订单状态
const formatOrderStatus = (status: string) => {
  const statusMap: Record<string, string> = {
    'PENDING': '待支付',
    'PAID': '已支付',
    'CANCELLED': '已取消',
    'FAILED': '已失败'
  }
  return statusMap[status] || status
}

// 格式化支付方式
const formatPaymentMethod = (method: string) => {
  const methodMap: Record<string, string> = {
    'ALIPAY': '支付宝',
    'WECHAT': '微信支付'
  }
  return methodMap[method] || method
}

// 获取订单状态对应的标签类型
const getStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    'PENDING': 'warning',
    'PAID': 'success',
    'CANCELLED': 'info',
    'FAILED': 'danger'
  }
  return typeMap[status] || 'info'
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

// 获取商品列表
const fetchProducts = async () => {
  try {
    const res = await getProducts()
    products.value = res.data.data
  } catch (error) {
    ElMessage.error('获取商品列表失败')
  }
}
</script>

<template>
  <div class="dashboard-container">
    <div class="dashboard-content">
      <!-- 个人信息区域 -->
      <div class="profile-section">
        <div class="profile-header">
          <div class="user-info">
            <el-upload
                :auto-upload="true"
                :http-request="handleAvatarUpload"
                :show-file-list="false"
            >
              <el-avatar :size="100" :src="tempAvatar || userData.avatar">
                <template #default>
                  <UserFilled style="font-size: 40px"/>
                </template>
              </el-avatar>
              <template #tip>
                <div class="upload-tip">点击更换头像</div>
              </template>
            </el-upload>
            <div class="user-basic-info">
              <h2 class="user-title">
                <span>{{ userData.username }}</span>

                <!-- 认证标识 -->
                <user-badge :is-verified="userData.isVerified" :verified-name="userData.verifiedName"/>

                <!-- 申请按钮 -->
                <div class="auth-button-container">
                  <el-button
                      class="auth-apply-btn"
                      :disabled="userData.isVerified"
                      @click="applyDialogVisible = true"
                  >
                    {{
                      userData.isVerified
                          ? '已认证'
                          : '发起大师认证申请'
                    }}
                    <div v-if="!userData.isVerified" class="glow-effect"></div>
                  </el-button>
                  <el-button
                      type="info"
                      class="record-btn"
                      @click="recordDialogVisible = true; fetchVerificationRecords()"
                  >
                    申请记录
                  </el-button>
                </div>
              </h2>

              <div class="social-stats">
                <el-button type="primary" class="follow-btn" @click="router.push('/following')">
                  我的关注 {{ userData.followingCount }}
                </el-button>
                <el-button type="danger" class="fans-btn" @click="router.push('/followers')">
                  我的粉丝 {{ userData.followerCount }}
                </el-button>
              </div>
              <p class="role-tag">{{ role }}</p>
              <div class="tomato-info">
                <el-icon>
                  <Food/>
                </el-icon>
                <span>{{ userData.tomato }} 圣女果</span>
                <el-button
                    type="primary"
                    size="small"
                    @click="showRechargeDialog = true"
                    class="recharge-btn"
                >
                  充值
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <div class="profile-body">
          <el-form
              :model="userData"
              :rules="rules"
              label-width="80px"
              class="profile-form"
              ref="formRef"
          >
            <div class="form-grid">
              <el-form-item label="姓名">
                <el-input v-model="userData.name" :disabled="!editMode"/>
              </el-form-item>

              <el-form-item label="手机号" prop="telephone">
                <el-input
                    v-model="userData.telephone"
                    :disabled="!editMode"
                    placeholder="1xxxxxxxxxx"
                />
              </el-form-item>

              <el-form-item label="邮箱" prop="email">
                <el-input
                    v-model="userData.email"
                    :disabled="!editMode"
                    placeholder="example@domain.com"
                />
              </el-form-item>

              <el-form-item label="地址">
                <el-input
                    v-model="userData.location"
                    :disabled="!editMode"
                    placeholder="请输入地址"
                />
              </el-form-item>
            </div>

            <el-form-item label="密码" prop="password">
              <div class="password-field">
                <el-input
                    v-model="userData.password"
                    :disabled="!editMode || !isChangingPassword"
                    type="password"
                    show-password
                    :placeholder="editMode ? '请输入新密码' : ''"
                    autocomplete="new-password"
                />
                <el-button
                    v-if="editMode"
                    @click="togglePasswordChange"
                    class="change-pwd-btn"
                    :type="isChangingPassword ? 'danger' : 'primary'"
                >
                  {{ isChangingPassword ? '取消修改' : '修改密码' }}
                </el-button>
              </div>
            </el-form-item>

            <el-form-item
                v-if="isChangingPassword"
                label="确认密码"
                prop="confirmPassword"
            >
              <el-input
                  v-model="userData.confirmPassword"
                  type="password"
                  show-password
                  placeholder="请再次输入新密码"
                  autocomplete="new-password"
              />
            </el-form-item>

            <div class="form-actions">
              <el-button
                  v-if="!userData.isVerified && role === 'user'"
                  type="warning"
                  @click="applyDialogVisible = true"
              >
                发起大师认证申请
              </el-button>
              <el-button
                  v-if="!editMode"
                  type="primary"
                  @click="editMode = true"
              >
                编辑信息
              </el-button>
              <template v-else>
                <el-button @click="cancelEdit">取消</el-button>
                <el-button type="primary" @click="handleSubmit">保存更改</el-button>
              </template>
            </div>
          </el-form>
        </div>
      </div>

      <!-- 书单展示区域 -->
      <div class="booklists-section">
        <div class="section-header">
          <h2>我的书单</h2>
          <div class="tabs">
            <div
                :class="['tab-item', { active: activeTab === 'created' }]"
                @click="activeTab = 'created'"
            >
              我创建的
            </div>
            <div
                :class="['tab-item', { active: activeTab === 'favourite' }]"
                @click="activeTab = 'favourite'"
            >
              我收藏的
            </div>
          </div>
        </div>

        <div class="booklists-grid" v-loading="loading">
          <book-list-item
              v-for="bookList in activeTab === 'created' ? createdBookLists : favouriteBookLists"
              :key="bookList.id"
              :book-list="bookList"
              :is-favourite="favouriteBookListIds.has(bookList.id)"
              :is-creator="currentUserId === bookList.creatorId"
              @collect="handleCollect"
              @delete="handleDelete"
              @view="handleView"
          />
        </div>

        <!-- 无数据提示 -->
        <div v-if="(activeTab === 'created' ? createdBookLists : favouriteBookLists).length === 0"
             class="no-data">
          暂无{{ activeTab === 'created' ? '创建' : '收藏' }}的书单
        </div>
      </div>

      <!-- 读书笔记区域 -->
      <div class="booklists-section">
        <div class="section-header">
          <h2>我的读书笔记</h2>
          <div class="tabs">
            <div
                :class="['tab-item', { active: activeNoteTab === 'created-notes' }]"
                @click="activeNoteTab = 'created-notes'"
            >
              我创建的
            </div>
            <div
                :class="['tab-item', { active: activeNoteTab === 'paid-notes' }]"
                @click="activeNoteTab = 'paid-notes'"
            >
              我购买的
            </div>
            <div
                :class="['tab-item', { active: activeNoteTab === 'liked-notes' }]"
                @click="activeNoteTab = 'liked-notes'"
            >
              我收藏的
            </div>
          </div>
        </div>

        <div class="booklists-grid" v-loading="loadingNotes">
          <ReadingNote
              @view="handleViewNote"
              v-for="note in activeNoteTab === 'created-notes'
          ? createdNotes
          : activeNoteTab === 'paid-notes'
            ? paidNotes
            : likedNotes"
              :key="note.id"
              :note="note"
              :is-liked="likedNoteIds.includes(note.id)"
              :is-creator="currentUserId === note.creatorId"
              :is-paid="paidNoteIds.has(note.id)"
              @like="handleLikeNote"
              @unlike="handleUnlikeNote"
              @delete="handleDeleteNote"
              @purchase="handlePurchaseNote"
          />
        </div>

        <div
            v-if="(activeNoteTab === 'created-notes' && createdNotes.length === 0)
        || (activeNoteTab === 'paid-notes' && paidNotes.length === 0)
        || (activeNoteTab === 'liked-notes' && likedNotes.length === 0)"
            class="no-data"
        >
          暂无{{
            activeNoteTab === 'created-notes' ? '创建' :
                activeNoteTab === 'paid-notes' ? '购买' : '收藏'
          }}的读书笔记
        </div>
      </div>

      <!-- 订单历史区域 -->
      <div class="orders-section">
        <div class="section-header">
          <h2>订单历史</h2>
        </div>

        <div class="orders-list" v-loading="loadingOrders">
          <div class="orders-scroll-container" v-if="orders.length > 0">
            <div v-for="order in orders" :key="order.id" class="order-card">
              <div class="order-header">
                <span class="order-id">订单号：{{ order.id }}</span>
                <el-tag :type="getStatusType(order.status)" size="small">
                  {{ formatOrderStatus(order.status) }}
                </el-tag>
              </div>

              <div class="order-content">
                <div class="order-info-item">
                  <span class="label">订单金额</span>
                  <span class="amount">¥{{ order.totalAmount.toFixed(2) }}</span>
                  <span v-if="order.useCoupon" class="discount-info">
                    (优惠: ¥{{ order.reducedAmount.toFixed(2) }})
                  </span>
                </div>

                <div class="order-info-item">
                  <span class="label">支付方式</span>
                  <span class="value">{{ formatPaymentMethod(order.paymentMethod) }}</span>
                </div>

                <div class="order-info-item">
                  <span class="label">创建时间</span>
                  <span class="value">{{ new Date(order.createTime).toLocaleString() }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 无数据提示 -->
          <div v-if="orders.length === 0" class="no-data">
            暂无订单记录
          </div>
        </div>
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

          <!-- 修改内容展示部分 -->
          <div class="note-content-container">
            <div
                class="note-content"
                :class="{ 'limited-content': currentNote.price > 0 && !paidNoteIds.has(currentNote.id) }"
                style="white-space: pre-wrap;"
            >
              {{ getDisplayContent(currentNote.content, paidNoteIds.has(currentNote.id)) }}
            </div>

            <!-- 未购买提示 -->
            <div
                v-if="currentNote.price > 0 && !paidNoteIds.has(currentNote.id)"
                class="purchase-tip"
            >
              <el-alert
                  title="预览内容已结束，购买后可查看完整笔记"
                  type="warning"
                  :closable="false"
                  show-icon
              />
              <el-button
                  type="primary"
                  class="purchase-button"
                  @click="handlePurchaseNote(currentNote)"
              >
                立即解锁（{{ currentNote.price }} 🍅）
              </el-button>
            </div>
          </div>

          <div class="actions" v-if="currentUserId === currentNote.creatorId" style="margin-top: 20px;">
            <el-button type="primary" @click="handleEditNote(currentNote)">编辑笔记</el-button>
            <el-button type="danger" @click="handleDeleteNote(currentNote.id)">删除笔记</el-button>
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
            <el-input v-model="editNoteForm.title"/>
          </el-form-item>
          <el-form-item label="内容" required>
            <el-input
                v-model="editNoteForm.content"
                type="textarea"
                :rows="4"
                resize="none"
            />
          </el-form-item>
          <el-form-item label="价格">
            <el-input-number
                v-model="editNoteForm.price"
                :min="0"
                :precision="0"
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
    </div>

    <!-- 重新登录对话框 -->
    <el-dialog
        v-model="showReloginDialog"
        title="安全提示"
        width="30%"
        :close-on-click-modal="false"
        :show-close="false"
    >
      <span>密码已修改，请重新登录以确保账户安全</span>
      <template #footer>
        <el-button type="primary" @click="handleRelogin">重新登录</el-button>
      </template>
    </el-dialog>

    <!-- 添加申请记录对话框 -->
    <el-dialog
        v-model="recordDialogVisible"
        title="认证申请记录"
        width="80%"
    >
      <div v-loading="loadingRecords">
        <el-table :data="verificationRecords" style="width: 100%">
          <el-table-column prop="createTime" label="申请时间" width="180">
            <template #default="{row}">
              {{ new Date(row.createTime).toLocaleString() }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{row}">
              <el-tag :type="row.status === 'APPROVED' ? 'success' : row.status === 'REJECTED' ? 'danger' : 'warning'">
                {{ row.status === 'PENDING' ? '审核中' : row.status === 'APPROVED' ? '已通过' : '已拒绝' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="verifiedName" label="申请名号" width="150"/>
          <el-table-column label="申请理由" min-width="180">
            <template #default="{row}">
              <div class="reason-text">
                {{ row.reasonText || '无申请理由' }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="证明材料" width="200">
            <template #default="{row}">
              <div class="proof-images" v-if="row.proofImgs?.length">
                <el-image
                    v-for="(img, index) in row.proofImgs"
                    preview-teleported
                    :key="index"
                    :src="img"
                    :preview-src-list="row.proofImgs"
                    :initial-index="index"
                    style="width: 60px; height: 60px; margin-right: 5px;"
                    fit="cover"
                    hide-on-click-modal
                />
              </div>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="审核意见" min-width="180">
            <template #default="{row}">
              <div v-if="row.status === 'REJECTED'">
                <div class="reject-reason">{{ row.rejectReason }}</div>
              </div>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="reviewTime" label="审核时间" width="180">
            <template #default="{row}">
              {{ row.reviewTime ? new Date(row.reviewTime).toLocaleString() : '-' }}
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-container">
          <el-pagination
              :current-page="currentPage"
              :page-size="pageSize"
              :total="totalRecords"
              layout="prev, pager, next"
              @current-change="handlePageChange"
          />
        </div>
      </div>
    </el-dialog>

    <!-- 添加认证申请对话框 -->
    <el-dialog v-model="applyDialogVisible" title="大师认证申请">
      <el-form
          :model="applyForm"
          :rules="applyRules"
          ref="formRef"
          label-width="100px">
        <!-- 新增认证名号选择 -->
        <el-form-item label="认证名号" prop="verifiedName">
          <el-select
              v-model="applyForm.verifiedName"
              placeholder="请选择认证名号"
              class="full-width"
          >
            <el-option
                v-for="name in verifiedNameOptions"
                :key="name"
                :label="name"
                :value="name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="申请理由" required>
          <el-input
              v-model="applyForm.reasonText"
              type="textarea"
              :rows="4"
              placeholder="请说明申请理由（至少20字）"
          />
        </el-form-item>
        <el-form-item label="证明材料">
          <el-upload
              list-type="picture-card"
              :auto-upload="true"
              :http-request="handleProofUpload"
              :on-remove="handleProofRemove"
              :file-list="proofFiles"
              :before-upload="beforeProofUpload"
          >
            <el-icon>
              <Plus/>
            </el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleApplyVerification">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 圣女果充值对话框 -->
    <el-dialog
        v-model="showRechargeDialog"
        title="圣女果充值"
        width="500px"
        class="recharge-dialog"
    >
      <div class="recharge-content">
        <div class="recharge-header">
          <el-icon class="tomato-icon">
            <Food/>
          </el-icon>
          <h3>圣女果充值</h3>
          <p class="current-balance">当前余额：{{ userData.tomato }} 圣女果</p>
        </div>

        <el-form
            ref="rechargeFormRef"
            :model="{ amount: rechargeAmount }"
            :rules="rechargeRules"
            label-width="0"
            class="recharge-form"
        >
          <el-form-item prop="amount">
            <div class="amount-input-wrapper">
              <span class="amount-label">充值数量</span>
              <el-input-number
                  v-model="rechargeAmount"
                  :min="1"
                  :step="1"
                  :precision="0"
                  size="large"
                  class="amount-input"
                  placeholder="请输入充值数量"
              />
            </div>
          </el-form-item>

          <div class="quick-amounts">
            <el-button
                v-for="amount in [10, 50, 100, 200]"
                :key="amount"
                :class="['quick-amount-btn', { active: rechargeAmount === amount }]"
                @click="rechargeAmount = amount"
            >
              {{ amount }}个
            </el-button>
          </div>

          <div class="payment-method">
            <span class="method-label">支付方式</span>
            <div class="method-options">
              <div class="method-option active">
                <el-icon>
                  <Money/>
                </el-icon>
                <span>支付宝</span>
              </div>
            </div>
          </div>
        </el-form>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <div class="total-amount">
            <span>总计：</span>
            <span class="amount">¥{{ calculateTotalAmount }}</span>
          </div>
          <div class="action-buttons">
            <el-button @click="showRechargeDialog = false">取消</el-button>
            <el-button
                type="primary"
                @click="handleRecharge"
                :disabled="!rechargeAmount"
            >
              立即充值
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dashboard-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 2rem;
}

.dashboard-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

/* 个人信息区域样式 */
.profile-section {
  background: linear-gradient(135deg, #2c698d 0%, #272643 100%);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.profile-header {
  padding: 2rem;
  color: white;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 2rem;
}

.user-basic-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.user-basic-info h2 {
  margin: 0;
  font-size: 1.8rem;
  font-weight: 500;
}

.role-tag {
  background: rgba(255, 255, 255, 0.2);
  padding: 0.25rem 1rem;
  border-radius: 20px;
  font-size: 0.9rem;
  margin: 0;
  width: 50px
}

.tomato-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
  color: #ff6b6b;
  font-size: 1rem;
  font-weight: 500;
}

.tomato-info .el-icon {
  font-size: 1.4rem;
  color: #ff6b6b;
  transform: rotate(-15deg);
}

.upload-tip {
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.9rem;
  margin-top: 0.5rem;
  text-align: center;
}

.profile-body {
  background: white;
  padding: 2rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}

.profile-form {
  max-width: 100%;
}

:deep(.el-form-item__label) {
  color: #2c698d;
  font-weight: 500;
}

:deep(.el-input__inner) {
  border-color: #bae8e8;
}

.password-field {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.change-pwd-btn {
  flex-shrink: 0;
}

.form-actions {
  margin-top: 2rem;
  text-align: center;
}

.el-button {
  transition: all 0.3s ease;
  border-radius: 8px;
  padding: 12px 24px;
}

.el-button--primary {
  background-color: #2c698d;
  border-color: #2c698d;
}

.el-button--primary:hover {
  background-color: #272643;
  border-color: #272643;
}

/* 书单区域样式 */
.booklists-section {
  background: white;
  border-radius: 16px;
  padding: 2rem;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.section-header h2 {
  color: #2c698d;
  font-size: 1.5rem;
  margin: 0;
}

.tabs {
  display: flex;
  gap: 1rem;
}

.tab-item {
  padding: 0.5rem 1rem;
  cursor: pointer;
  color: #606266;
  position: relative;
  transition: all 0.3s;
}

.tab-item:hover {
  color: #2c698d;
}

.tab-item.active {
  color: #2c698d;
  font-weight: 500;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 0;
  width: 100%;
  height: 2px;
  background-color: #2c698d;
}

.booklists-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

.booklist-card {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 1.5rem;
  cursor: pointer;
  transition: all 0.3s;
}

.booklist-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.booklist-header {
  margin-bottom: 1rem;
}

.booklist-header h3 {
  margin: 0 0 0.5rem 0;
  color: #2c698d;
  font-size: 1.1rem;
}

.creator-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #606266;
  font-size: 0.9rem;
}

.description {
  color: #606266;
  font-size: 0.9rem;
  margin: 0 0 1rem 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.booklist-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #909399;
  font-size: 0.9rem;
}

.favourite-count {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.no-data {
  text-align: center;
  padding: 3rem;
  color: #909399;
  font-size: 1rem;
}

/* 书单详情样式 */
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
  position: relative;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.product-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.product-cover {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 4px;
}

.product-info {
  flex: 1;
}

.product-info h4 {
  margin: 0;
  font-size: 14px;
  color: #2c3e50;
}

.price {
  color: #f56c6c;
  font-weight: bold;
  margin: 4px 0;
}

.verified-tag {
  margin-left: 10px;
  vertical-align: middle;
}

.social-stats {
  margin-top: 8px;
  color: #666;
  font-size: 0.9em;
}

.social-stats span {
  margin-right: 15px;
}

.badge {
  margin-left: 8px;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  height: 24px;
  line-height: 20px;
  letter-spacing: 0.5px;
  border-width: 1px;
}

/* 认证标识优化 */
.verified-badge {
  background: linear-gradient(45deg, #FFD700 0%, #FFC800 100%);
  border-color: #D4AF37;
  color: #2D2D2D;

  .v-icon {
    color: #2D2D2D;
    font-size: 14px;
    margin-right: 4px;
    vertical-align: -1px;
  }

  .badge-text {
    font-weight: 500;
  }
}

/* 读者标识优化 */
.reader-badge {
  background: rgba(64, 158, 255, 0.08);
  border-color: rgba(64, 158, 255, 0.6);
  color: #409EFF;
  font-weight: 400;
}

/* 调整Element图标颜色 */
:deep(.el-icon) {
  vertical-align: middle;
}

.verified-badge {
  position: relative;
  background: linear-gradient(
      45deg,
      #FFD700 0%,
      #FFC800 30%,
      #D4AF37 70%,
      #FFD700 100%
  );
  border: 1px solid rgba(212, 175, 55, 0.8);
  color: #2d2d2d;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: linear-gradient(
        45deg,
        rgba(255, 255, 255, 0) 25%,
        rgba(255, 255, 255, 0.8) 50%,
        rgba(255, 255, 255, 0) 75%
    );
    animation: metal-glow 2s infinite linear;
  }
}

/* 动态V图标 */
.v-icon {
  width: 18px;
  height: 18px;
  margin-right: 6px;
  filter: drop-shadow(0 0 2px rgba(255, 215, 0, 0.8));
  animation: icon-float 1.5s ease-in-out infinite,
  icon-glow 1s alternate infinite;
}

/* 关键帧动画 */
@keyframes metal-glow {
  0% {
    transform: translate(-25%, -25%) rotate(45deg);
  }
  100% {
    transform: translate(25%, 25%) rotate(45deg);
  }
}

@keyframes icon-float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-3px);
  }
}

@keyframes icon-glow {
  from {
    filter: drop-shadow(0 0 2px rgba(255, 215, 0, 0.8));
  }
  to {
    filter: drop-shadow(0 0 5px rgba(255, 215, 0, 0.9));
  }
}

/* 悬停增强效果 */
.verified-badge:hover {
  transform: scale(1.05);
  box-shadow: 0 0 15px rgba(255, 215, 0, 0.5),
  0 0 30px rgba(255, 215, 0, 0.3);
}

/* 过渡效果 */
.glow-enter-active {
  animation: glow-in 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes glow-in {
  0% {
    opacity: 0;
    transform: scale(0.8) rotate(-5deg);
  }
  100% {
    opacity: 1;
    transform: scale(1) rotate(0);
  }
}

.auth-apply-btn {
  position: relative;
  background: linear-gradient(45deg, #FF6B6B 0%, #FFE66D 100%);
  border: none;
  color: #2d3436;
  font-weight: 600;
  overflow: hidden;
  transition: all 0.3s ease;
  padding-right: 35px;

  &::after {
    content: '✨';
    position: absolute;
    right: 10px;
    top: 50%;
    transform: translateY(-50%);
  }

  &:hover {
    transform: scale(1.05);
    box-shadow: 0 4px 15px rgba(255, 107, 107, 0.4);

    .glow-effect {
      opacity: 1;
      transform: translateX(100%);
    }
  }
}

.glow-effect {
  position: absolute;
  top: 0;
  left: -50%;
  width: 50%;
  height: 100%;
  background: linear-gradient(
      90deg,
      rgba(255, 255, 255, 0) 0%,
      rgba(255, 255, 255, 0.3) 50%,
      rgba(255, 255, 255, 0) 100%
  );
  opacity: 0;
  transform: skewX(-20deg);
  transition: all 0.6s ease;
  animation: button-glow 2s infinite;
}

@keyframes button-glow {
  0% {
    opacity: 0;
  }
  50% {
    opacity: 0.8;
  }
  100% {
    opacity: 0;
  }
}

.user-title {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  flex-wrap: wrap;
}

.auth-button-container {
  margin-left: 800px; /* 用固定间距代替auto */
  order: 2; /* 确保按钮在标识之后 */
}


/* 调整原有认证标识样式 */
.verified-badge,
.reader-badge {
  order: 1; /* 确保标识在按钮之前 */
}

@media (max-width: 768px) {
  .user-title {
    flex-direction: column;
    align-items: flex-start;
  }

  .auth-button-container {
    margin-left: 0;
    order: 3;
    width: 100%;
  }
}

.recharge-btn {
  margin-left: 1rem;
  background-color: #ff6b6b;
  border-color: #ff6b6b;
}

.recharge-btn:hover {
  background-color: #ff5252;
  border-color: #ff5252;
}

.recharge-dialog :deep(.el-dialog__header) {
  margin: 0;
  padding: 20px 24px;
  border-bottom: 1px solid #ebeef5;
}

.recharge-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

.recharge-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.recharge-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid #ebeef5;
}

.recharge-content {
  padding: 24px;
}

.recharge-header {
  text-align: center;
  margin-bottom: 24px;
}

.tomato-icon {
  font-size: 48px;
  color: #ff6b6b;
  margin-bottom: 16px;
  transform: rotate(-15deg);
}

.recharge-header h3 {
  font-size: 20px;
  color: #2c3e50;
  margin: 0 0 8px 0;
}

.current-balance {
  color: #606266;
  font-size: 14px;
  margin: 0;
}

.amount-input-wrapper {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.amount-label {
  font-size: 16px;
  color: #2c3e50;
  font-weight: 500;
  min-width: 70px;
}

.amount-input {
  flex: 1;
}

.amount-input :deep(.el-input-number__decrease),
.amount-input :deep(.el-input-number__increase) {
  background-color: #f5f7fa;
}

.amount-input :deep(.el-input__inner) {
  text-align: center;
  font-size: 16px;
}

.quick-amounts {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.quick-amount-btn {
  height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  font-size: 14px;
  color: #606266;
  background-color: #fff;
  transition: all 0.3s;
}

.quick-amount-btn:hover {
  border-color: #ff6b6b;
  color: #ff6b6b;
}

.quick-amount-btn.active {
  background-color: #ff6b6b;
  border-color: #ff6b6b;
  color: #fff;
}

.payment-method {
  margin-top: 24px;
}

.method-label {
  display: block;
  font-size: 16px;
  color: #2c3e50;
  font-weight: 500;
  margin-bottom: 16px;
}

.method-options {
  display: flex;
  gap: 16px;
}

.method-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.method-option:hover {
  border-color: #ff6b6b;
}

.method-option.active {
  border-color: #ff6b6b;
  background-color: #fff5f5;
}

.method-option .el-icon {
  font-size: 20px;
  color: #ff6b6b;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.total-amount {
  font-size: 16px;
  color: #606266;
}

.total-amount .amount {
  font-size: 24px;
  color: #ff6b6b;
  font-weight: 600;
  margin-left: 8px;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.action-buttons .el-button {
  padding: 12px 24px;
  font-size: 14px;
}

.action-buttons .el-button--primary {
  background-color: #ff6b6b;
  border-color: #ff6b6b;
}

.action-buttons .el-button--primary:hover {
  background-color: #ff5252;
  border-color: #ff5252;
}

.action-buttons .el-button--primary:disabled {
  background-color: #ffb3b3;
  border-color: #ffb3b3;
}

/* 订单历史区域样式 */
.orders-section {
  background: white;
  border-radius: 16px;
  padding: 2rem;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  margin-top: 2rem;
}

.orders-list {
  margin-top: 1rem;
  position: relative;
}

.orders-scroll-container {
  display: flex;
  gap: 1rem;
  overflow-x: auto;
  padding: 1rem 0.5rem;
  scrollbar-width: thin;
  scrollbar-color: #c0c4cc #f5f7fa;
}

.orders-scroll-container::-webkit-scrollbar {
  height: 6px;
}

.orders-scroll-container::-webkit-scrollbar-track {
  background: #f5f7fa;
  border-radius: 3px;
}

.orders-scroll-container::-webkit-scrollbar-thumb {
  background-color: #c0c4cc;
  border-radius: 3px;
}

.order-card {
  flex: 0 0 300px;
  background: #f8f9fa;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  border: 1px solid #ebeef5;
}

.order-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #ebeef5;
}

.order-id {
  font-size: 0.9rem;
  color: #606266;
  font-weight: 500;
}

.order-content {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.order-info-item {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.order-info-item .label {
  font-size: 0.85rem;
  color: #909399;
}

.order-info-item .value {
  font-size: 0.95rem;
  color: #606266;
}

.order-info-item .amount {
  font-size: 1.1rem;
  color: #ff6b6b;
  font-weight: 500;
}

.order-info-item .discount-info {
  font-size: 0.85rem;
  color: #909399;
  margin-top: 0.2rem;
}

:deep(.el-tag) {
  border-radius: 4px;
  padding: 0 8px;
  height: 24px;
  line-height: 22px;
}

:deep(.el-tag--success) {
  background-color: #f0f9eb;
  border-color: #e1f3d8;
  color: #67c23a;
}

:deep(.el-tag--warning) {
  background-color: #fdf6ec;
  border-color: #faecd8;
  color: #e6a23c;
}

:deep(.el-tag--danger) {
  background-color: #fef0f0;
  border-color: #fde2e2;
  color: #f56c6c;
}

:deep(.el-tag--info) {
  background-color: #f4f4f5;
  border-color: #e9e9eb;
  color: #909399;
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

.record-btn {
  margin-left: 1px;
  background-color: #909399;
  border-color: #909399;
}

.record-btn:hover {
  background-color: #82848a;
  border-color: #82848a;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.reject-reason {
  color: #f56c6c;
  word-break: break-word;
}

.proof-images {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 5px;
}

.note-detail {
  padding: 20px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.detail-price {
  font-size: 18px;
  color: #e6a23c;
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-price.paid {
  color: #67c23a;
}

.paid-badge {
  background: #f0f9eb;
  color: #67c23a;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 14px;
}

.note-image {
  border-radius: 8px;
  margin-bottom: 16px;
}

.preview-image {
  max-width: 200px;
  border-radius: 4px;
  margin-top: 10px;
}

.note-content-container {
  position: relative;
}

.limited-content {
  position: relative;
  max-height: 200px;
  overflow: hidden;
}

.limited-content::after {
  content: "";
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: linear-gradient(transparent, white);
}

.purchase-tip {
  margin-top: 20px;
  text-align: center;
  border-top: 1px solid #eee;
  padding-top: 20px;
}

.purchase-button {
  margin-top: 15px;
  width: 100%;
}

.paid-badge {
  background: #f0f9eb;
  color: #67c23a;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 14px;
}

.social-stats {
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
}

.follow-btn {
  background: linear-gradient(45deg, #409EFF, #79BBFF);
  border: none;
  color: white;
  font-size: 1.1rem;
  padding: 6px 12px;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(64, 158, 255, 0.2);
}

.fans-btn {
  background: linear-gradient(45deg, #F56C6C, #F89898);
  border: none;
  color: white;
  font-size: 1.1rem;
  padding: 6px 12px;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(245, 108, 108, 0.2);
}

.follow-btn:hover, .fans-btn:hover {
  transform: translateY(-2px);
  transition: all 0.3s ease;
}
</style>
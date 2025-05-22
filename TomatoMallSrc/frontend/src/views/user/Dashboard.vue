<!-- Dashboard.vue -->
<script setup lang="ts">
import {ref, onMounted, computed} from 'vue'
import {useRouter} from 'vue-router'
import {getUserInfo, updateUserInfo} from '../../api/user'
import {ElMessage, ElLoading, ElDialog, ElMessageBox, type FormInstance} from 'element-plus'
import {UserFilled, Food, Money} from '@element-plus/icons-vue'
import {uploadUserImage} from '../../api/util'
import {getFavouriteBookLists, getAllBookLists, type BookListVO, collectBookList, cancelCollectBookList, deleteBookList} from '../../api/booklist'
import {submitTomatoRecharge, payOrder, alipayHelper} from '../../api/order'
import BookListItem from '../../components/BookListItem.vue'
import { applyVerification, type ApplyVerificationRequest  } from '../../api/verification'
import { ElTag } from 'element-plus'
import axios from "axios";
const sparkle = ref(false)
import { getVerificationListByStatus } from '../../api/verification'
import UserBadge from '../../components/UserBadge.vue'

const hasPendingVerification = ref(false)
const applyButtonText = ref('发起大师认证申请')
const isApplying = ref(false)
const startSparkle = () => {
  sparkle.value = true
  setTimeout(() => sparkle.value = false, 1000)
}

interface UploadResponse {
  data: string
}

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

// 圣女果充值相关
const showRechargeDialog = ref(false)
const rechargeAmount = ref(0)
const rechargeFormRef = ref<FormInstance>()
const rechargeRules = {
  amount: [
    { required: true, message: '请输入充值数量', trigger: 'blur' },
    { type: 'number', min: 1, message: '充值数量必须大于0', trigger: 'blur' }
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
    { required: true, message: '请输入申请理由', trigger: 'blur' },
    { min: 20, message: '申请理由至少20字', trigger: 'blur' }
  ],
  verifiedName: [
    { required: true, message: '请选择认证名号', trigger: 'change' }
  ]
}

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

    // 如果已经认证，不需要检查审核状态
    if (!userData.value.isVerified) {
      const pendingRes = await getVerificationListByStatus('PENDING', 0, 1)
      hasPendingVerification.value = pendingRes.data.data?.content?.length > 0 || false
    } else {
      hasPendingVerification.value = false
    }

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

// 处理证明材料上传
const handleProofUpload = async (params: any) => {
  const loading = ElLoading.service({ fullscreen: false })
  try {
    const { file } = params
    const response = await uploadUserImage(file)
    applyForm.value.proofImgs.push(response.data.data)
    proofFiles.value = applyForm.value.proofImgs.map(url => ({ url }))
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

    await applyVerification(request)

    await fetchUserInfo()
    ElMessage.success('申请已提交，请等待审核')
    applyDialogVisible.value = false
    applyForm.value = { reasonText: '', proofImgs: [], verifiedName: '' }
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
      await cancelCollectBookList({ bookListId: bookList.id })
      bookList.favouriteCount--
      favouriteBookListIds.value.delete(bookList.id)
      ElMessage.success('取消收藏成功')
    } else {
      await collectBookList({ bookListId: bookList.id })
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
                      :disabled="userData.isVerified || hasPendingVerification"
                      @click="applyDialogVisible = true"
                  >
                    {{
                      userData.isVerified
                          ? '已认证'
                          : hasPendingVerification
                              ? '审核中'
                              : '发起大师认证申请'
                    }}
                    <div v-if="!userData.isVerified && !hasPendingVerification" class="glow-effect"></div>
                  </el-button>
                </div>
              </h2>

              <div class="social-stats">
                <span>关注 {{ userData.followingCount }}</span>
                <span>粉丝 {{ userData.followerCount }}</span>
              </div>
              <p class="role-tag">{{ role }}</p>
              <div class="tomato-info">
                <el-icon><Food /></el-icon>
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
            </div>
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
            <el-icon><Plus /></el-icon>
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
          <el-icon class="tomato-icon"><Food /></el-icon>
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
                <el-icon><Money /></el-icon>
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
  background: rgba(64,158,255,0.08);
  border-color: rgba(64,158,255,0.6);
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
        rgba(255,255,255,0) 25%,
        rgba(255,255,255,0.8) 50%,
        rgba(255,255,255,0) 75%
    );
    animation: metal-glow 2s infinite linear;
  }
}

/* 动态V图标 */
.v-icon {
  width: 18px;
  height: 18px;
  margin-right: 6px;
  filter: drop-shadow(0 0 2px rgba(255,215,0,0.8));
  animation:
      icon-float 1.5s ease-in-out infinite,
      icon-glow 1s alternate infinite;
}

/* 关键帧动画 */
@keyframes metal-glow {
  0% { transform: translate(-25%, -25%) rotate(45deg); }
  100% { transform: translate(25%, 25%) rotate(45deg); }
}

@keyframes icon-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}

@keyframes icon-glow {
  from { filter: drop-shadow(0 0 2px rgba(255,215,0,0.8)); }
  to { filter: drop-shadow(0 0 5px rgba(255,215,0,0.9)); }
}

/* 悬停增强效果 */
.verified-badge:hover {
  transform: scale(1.05);
  box-shadow:
      0 0 15px rgba(255,215,0,0.5),
      0 0 30px rgba(255,215,0,0.3);
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
    box-shadow: 0 4px 15px rgba(255,107,107,0.4);

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
      rgba(255,255,255,0) 0%,
      rgba(255,255,255,0.3) 50%,
      rgba(255,255,255,0) 100%
  );
  opacity: 0;
  transform: skewX(-20deg);
  transition: all 0.6s ease;
  animation: button-glow 2s infinite;
}

@keyframes button-glow {
  0% { opacity: 0; }
  50% { opacity: 0.8; }
  100% { opacity: 0; }
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
</style>
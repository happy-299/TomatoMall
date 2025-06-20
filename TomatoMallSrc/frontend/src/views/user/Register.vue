<script setup lang="ts">
import { ElMessage, type FormInstance } from 'element-plus'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { userRegister } from '../../api/user.ts'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

// 表单数据
const formData = ref({
  username: '',
  password: '',
  confirmPassword: '',
  name: '',
  role:'',
  avatar: '',
  telephone: '',
  email: '',
  location: ''
})

// 增强的验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9]{4,20}$/,
      message: '4-20位字母数字组合',
      trigger: 'blur'
    }
  ],
  name: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 10, message: '长度在2-10个字符', trigger: 'blur' }
  ],
  telephone: [
    { pattern: /^1(3[0-9]|4[579]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[189])\d{8}$/,
      message: '请输入有效的手机号码',
      trigger: 'blur'
    }
  ],
  role: [
    { required: true, message: '请选择用户身份', trigger: 'change' }
  ],
  email: [
    {
      type: 'email',
      pattern:/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
      message: '请输入有效的邮箱地址',
      trigger: ['blur', 'change']
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value !== formData.value.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 提交逻辑保持不变
const handleRegister = async (formEl: FormInstance | undefined) => {
  if (!formEl) return
  try {
    await formEl.validate()
    loading.value = true

    const { confirmPassword, ...submitData } = formData.value
    console.log(submitData)
    const res = await userRegister(submitData)
    console.log(res)
    console.log(res.data.code)
    if (res.data.code === '200') {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      ElMessage.warning(res.data.data.msg || '注册失败')
    }
  } catch (error: any) {
    if (error?.response?.data?.code === '400') {
      ElMessage.error(error.response.data.data.msg)
    } else {
      ElMessage.error('用户名已存在，请重新输入！')
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-container">
    <div class="tomatoes-bg">
      <div class="tomato"></div>
      <div class="tomato"></div>
      <div class="tomato"></div>
      <div class="tomato"></div>
      <div class="tomato"></div>
      <div class="tomato"></div>
      <div class="tomato"></div>
      <div class="tomato"></div>
      <div class="tomato"></div>
      <div class="tomato"></div>
    </div>
    
    <!-- 左侧标题区域 -->
    <div class="left-section">
      <div class="brand-area">
        <div class="logo-container">
          <div class="tomato-logo">🍅</div>
          <div class="book-icon">📚</div>
        </div>
        <h1 class="brand-title">番茄书城</h1>
        <p class="brand-subtitle">知识如番茄，营养又美味</p>
      </div>
      
      <!-- 装饰元素 -->
      <div class="decorations">
        <div class="floating-book book-1">📖</div>
        <div class="floating-book book-2">📗</div>
        <div class="floating-book book-3">📘</div>
        <div class="floating-tomato tomato-1">🍅</div>
        <div class="floating-tomato tomato-2">🍅</div>
        <div class="floating-leaf leaf-1">🌿</div>
        <div class="floating-leaf leaf-2">🌱</div>
      </div>
    </div>
    
    <!-- 右侧注册区域 -->
    <div class="right-section">
      <el-card class="register-card">
        <h1 class="title">创建一个新的账户</h1>

        <el-form
            class="custom-form"
            :model="formData"
            :rules="rules"
            ref="formRef"
        >
          <div class="form-row">
            <!-- 用户名 -->
            <el-form-item prop="username" class="form-item">
              <label>用户名</label>
              <el-input
                  v-model="formData.username"
                  placeholder="4-20位字母、数字组合"
              />
            </el-form-item>

            <!-- 真实姓名 -->
            <el-form-item prop="name" class="form-item">
              <label>姓名</label>
              <el-input
                  v-model="formData.name"
                  placeholder="请输入姓名"
              />
            </el-form-item>
          </div>

          <div class="form-row">
            <!-- 修改后的身份选择部分 -->
            <el-form-item prop="role" class="form-item">
              <label>身份</label>
              <el-select
                  v-model="formData.role"
                  placeholder="请选择身份"
                  style="width: 100%"
              >
                <el-option
                    label="顾客"
                    value="user"
                />
                <el-option
                    label="管理员"
                    value="admin"
                />
              </el-select>
            </el-form-item>

            <!-- 手机号 -->
            <el-form-item prop="telephone" class="form-item">
              <label>手机号</label>
              <el-input
                  v-model="formData.telephone"
                  placeholder="1xxxxxxxxxx"
              />
            </el-form-item>
          </div>

          <div class="form-row">
            <!-- 邮箱 -->
            <el-form-item prop="email" class="form-item">
              <label>邮箱</label>
              <el-input
                  v-model="formData.email"
                  placeholder="example@domain.com"
              />
            </el-form-item>

            <!-- 地址 -->
            <el-form-item prop="location" class="form-item">
              <label>地址</label>
              <el-input
                  v-model="formData.location"
                  placeholder="请输入地址"
              />
            </el-form-item>
          </div>

          <div class="form-row">
            <!-- 密码 -->
            <el-form-item prop="password" class="form-item">
              <label>密码</label>
              <el-input
                  type="password"
                  v-model="formData.password"
                  placeholder="••••••••"
                  show-password
              />
            </el-form-item>

            <!-- 确认密码 -->
            <el-form-item prop="confirmPassword" class="form-item">
              <label>确认密码</label>
              <el-input
                  type="password"
                  v-model="formData.confirmPassword"
                  placeholder="••••••••"
                  show-password
              />
            </el-form-item>
          </div>

          <div class="action-group">
            <el-button
                type="primary"
                :loading="loading"
                @click.prevent="handleRegister(formRef)"
            >
              创建账户
            </el-button>

            <router-link to="/login" class="login-link">
              <el-button>去登录</el-button>
            </router-link>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
/* Animated background */
.tomatoes-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  overflow: hidden;
}

.tomato {
  position: absolute;
  top: -150px;
  background-color: #ff6347; /* Tomato color */
  border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%;
  animation: fall 10s infinite linear;
}

.tomato::before {
  content: '';
  position: absolute;
  top: -5px;
  left: 50%;
  transform: translateX(-50%);
  width: 4px;
  height: 8px;
  background-color: #2e8b57; /* Green stem */
  border-radius: 4px 4px 0 0;
}

@keyframes fall {
  from {
    transform: translateY(0) rotate(0deg);
  }
  to {
    transform: translateY(110vh) rotate(360deg);
  }
}

/* Different sizes and positions for tomatoes */
.tomato:nth-child(1) { width: 20px; height: 18px; left: 10%; animation-duration: 15s; animation-delay: -5s; }
.tomato:nth-child(2) { width: 30px; height: 27px; left: 20%; animation-duration: 10s; animation-delay: -10s; }
.tomato:nth-child(3) { width: 15px; height: 13.5px; left: 30%; animation-duration: 12s; animation-delay: -3s; }
.tomato:nth-child(4) { width: 25px; height: 22.5px; left: 40%; animation-duration: 8s; animation-delay: 0s; }
.tomato:nth-child(5) { width: 18px; height: 16px; left: 50%; animation-duration: 18s; animation-delay: -7s; }
.tomato:nth-child(6) { width: 22px; height: 20px; left: 60%; animation-duration: 9s; animation-delay: -12s; }
.tomato:nth-child(7) { width: 32px; height: 29px; left: 70%; animation-duration: 11s; animation-delay: -1s; }
.tomato:nth-child(8) { width: 16px; height: 14.5px; left: 80%; animation-duration: 14s; animation-delay: -8s; }
.tomato:nth-child(9) { width: 28px; height: 25px; left: 90%; animation-duration: 7s; animation-delay: -4s; }
.tomato:nth-child(10) { width: 12px; height: 11px; left: 5%; animation-duration: 20s; animation-delay: -2s; }

.register-container {
  display: flex;
  min-height: 100vh;
  background-color: #fff0f0; /* Light pink/tomato-ish background */
  position: relative;
  overflow: hidden;
}

/* 左侧区域样式 */
.left-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
  z-index: 2;
  padding: 5px;
}

.brand-area {
  text-align: center;
  margin-bottom: 60px;
}

.logo-container {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  margin-bottom: 30px;
}

.tomato-logo {
  font-size: 60px;
  animation: bounce 2s infinite;
}

.book-icon {
  font-size: 50px;
  animation: float 3s ease-in-out infinite;
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-10px);
  }
  60% {
    transform: translateY(-5px);
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-10px);
  }
}

.brand-title {
  font-size: 48px;
  font-weight: bold;
  color: #d9534f;
  margin-bottom: 15px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
  font-family: 'Microsoft YaHei', 'Arial', sans-serif;
}

.brand-subtitle {
  font-size: 18px;
  color: #666;
  font-style: italic;
  margin: 0;
}

/* 装饰元素样式 */
.decorations {
  position: absolute;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.floating-book, .floating-tomato, .floating-leaf {
  position: absolute;
  font-size: 24px;
  animation: float-around 6s ease-in-out infinite;
}

.floating-book {
  animation-duration: 8s;
}

.floating-tomato {
  animation-duration: 7s;
}

.floating-leaf {
  animation-duration: 9s;
}

@keyframes float-around {
  0%, 100% {
    transform: translate(0, 0) rotate(0deg);
  }
  25% {
    transform: translate(20px, -20px) rotate(90deg);
  }
  50% {
    transform: translate(-10px, -30px) rotate(180deg);
  }
  75% {
    transform: translate(-20px, -10px) rotate(270deg);
  }
}

.book-1 { top: 20%; left: 15%; animation-delay: 0s; }
.book-2 { top: 60%; left: 25%; animation-delay: -2s; }
.book-3 { top: 80%; left: 10%; animation-delay: -4s; }
.tomato-1 { top: 30%; right: 20%; animation-delay: -1s; }
.tomato-2 { top: 70%; right: 15%; animation-delay: -3s; }
.leaf-1 { top: 40%; left: 5%; animation-delay: -1.5s; }
.leaf-2 { top: 90%; right: 25%; animation-delay: -2.5s; }

/* 右侧注册区域样式 */
.right-section {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2;
  padding: 5px;
}

.register-card {
  width: 500px;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(212, 83, 83, 0.2); /* Tomato-colored shadow */
  background-color: rgba(255, 255, 255, 0.95);
  z-index: 2;
  position: relative;
  backdrop-filter: blur(10px);
}

.title {
  color: #d9534f; /* Tomato Red */
  text-align: center;
  margin-bottom: 2rem;
  font-size: 1.5rem;
  font-weight: 600;
  font-family: 'Microsoft YaHei', 'Arial', sans-serif;
}

.custom-form :deep(.el-form-item__label) {
  color: #555;
  font-weight: 500;
  height: 44px;
  margin-bottom: 8px;
  display: block;
}

.custom-form :deep(.el-input__inner) {
  border: 1px solid #ddd;
  height: 44px;
  border-radius: 4px;
  padding: 0 15px;
  color: #333;
  font-size: 14px;
  background-color: #fff;
  transition: border-color 0.3s, box-shadow 0.3s;
}

.custom-form :deep(.el-input__inner:focus) {
  border-color: #d9534f;
  box-shadow: 0 0 0 2px rgba(217, 83, 79, 0.2);
}

.custom-form :deep(.el-select .el-input__inner) {
  border: 1px solid #ddd;
}

.custom-form :deep(.el-select .el-input__inner:focus) {
  border-color: #d9534f;
  box-shadow: 0 0 0 2px rgba(217, 83, 79, 0.2);
}

/* 移除Element Plus默认的输入框样式 */
.custom-form :deep(.el-input__wrapper) {
  border: none !important;
  box-shadow: none !important;
  background: transparent !important;
}

.custom-form :deep(.el-input__wrapper.is-focus) {
  border: none !important;
  box-shadow: none !important;
}

.custom-form :deep(.el-select .el-input__wrapper) {
  border: none !important;
  box-shadow: none !important;
  background: transparent !important;
}

.custom-form :deep(.el-select .el-input__wrapper.is-focus) {
  border: none !important;
  box-shadow: none !important;
}

/* 表单布局优化 */
.form-row {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.form-item {
  flex: 1;
  margin-bottom: 0;
}

.custom-form :deep(.form-item .el-form-item__content) {
  margin-left: 0 !important;
}

.custom-form :deep(.form-item .el-form-item__error) {
  position: absolute;
  top: 100%;
  left: 0;
  font-size: 12px;
  color: #f56c6c;
}

/* 响应式表单布局 */
@media (max-width: 768px) {
  .form-row {
    flex-direction: column;
    gap: 15px;
    margin-bottom: 15px;
  }
  
  .form-item {
    margin-bottom: 0;
  }
}

.action-group {
  margin-top: 1.5rem;
  display: flex;
  gap: 1.2rem;
  justify-content: flex-end;
}

.el-button {
  height: 44px;
  padding: 0 2rem;
  border-radius: 8px;
  transition: all 0.3s;
}

.el-button--primary {
  background: #d9534f; /* Tomato Red */
  border-color: #d9534f;
  color: white;
}

.el-button--primary:hover {
  background: #c9302c; /* Darker Tomato Red */
  border-color: #c12e2a;
  color: #ffffff;
}

.login-link .el-button {
  border-color: #28a745; /* Tomato Leaf Green */
  color: #28a745;
}

.login-link .el-button:hover {
  background-color: #28a745;
  border-color: #28a745;
  color: #ffffff;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .register-container {
    flex-direction: column;
  }
  
  .left-section {
    padding: 5px;
    margin-bottom: 5px;
  }
  
  .brand-title {
    font-size: 36px;
  }
  
  .brand-subtitle {
    font-size: 16px;
  }
  
  .right-section {
    padding: 5px;
  }
  
  .register-card {
    width: 100%;
    max-width: 450px;
    padding: 30px;
  }
  
  .action-group {
    flex-direction: column;
    gap: 1rem;
  }
}
</style>
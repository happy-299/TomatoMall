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
  avatar: '',
  phone: '',
  email: '',
  address: ''
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
    { pattern: /^1[3-9]\d{9}$/, // 改为非必填但保留格式校验
      message: '请输入有效的手机号码',
      trigger: 'blur'
    }
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
      validator: (rule: any, value: string, callback: any) => {
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
      ElMessage.error('请求异常，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-main class="main-frame bgimage">
    <el-card class="register-card">
      <h1 class="title">创建一个新的账户</h1>

      <el-form
          class="custom-form"
          :model="formData"
          :rules="rules"
          ref="formRef"
      >
        <!-- 用户名 -->
        <el-form-item prop="username">
          <label>用户名</label>
          <el-input
              v-model="formData.username"
              placeholder="4-20位字母、数字组合"
          />
        </el-form-item>

        <!-- 真实姓名 -->
        <el-form-item prop="name">
          <label>真实姓名</label>
          <el-input
              v-model="formData.name"
              placeholder="请输入真实姓名"
          />
        </el-form-item>

        <!-- 手机号 -->
        <el-form-item prop="telephone">
          <label>手机号（可选）</label>
          <el-input
              v-model="formData.phone"
              placeholder="1xxxxxxxxxx"
          />
        </el-form-item>

        <!-- 邮箱 -->
        <el-form-item prop="email">
          <label>邮箱（可选）</label>
          <el-input
              v-model="formData.email"
              placeholder="example@domain.com"
          />
        </el-form-item>

        <!-- 地址 -->
        <el-form-item prop="location">
          <label>地址（可选）</label>
          <el-input
              v-model="formData.address"
              placeholder="请输入地址"
          />
        </el-form-item>

        <!-- 头像 -->
        <el-form-item prop="avatar">
          <label>头像URL（可选）</label>
          <el-input
              v-model="formData.avatar"
              placeholder="输入图片链接地址"
          />
        </el-form-item>

        <!-- 密码 -->
        <el-form-item prop="password">
          <label>密码</label>
          <el-input
              type="password"
              v-model="formData.password"
              placeholder="••••••••"
              show-password
          />
        </el-form-item>

        <!-- 确认密码 -->
        <el-form-item prop="confirmPassword">
          <label>确认密码</label>
          <el-input
              type="password"
              v-model="formData.confirmPassword"
              placeholder="••••••••"
              show-password
          />
        </el-form-item>

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
  </el-main>
</template>

<!-- 样式部分保持不变 -->

<style scoped>
.main-frame {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(120deg, #e3f6f5 0%, #b8e1ff 100%);
}

.register-card {
  width: 500px;
  padding: 40px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 4px 20px rgba(14, 31, 53, 0.1);
}

.title {
  color: #272643;
  text-align: center;
  margin-bottom: 2rem;
  font-size: 1.5rem;
  font-weight: 600;
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
  background: #272643;
  border-color: #272643;
  color: white;
}

.el-button--primary:hover {
  background: #bae8e8;
  border-color: #bae8e8;
  color: #272643;
}

.login-link .el-button {
  border-color: #272643;
  color: #272643;
}
</style>
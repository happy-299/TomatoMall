<!-- Dashboard.vue -->
<script setup lang="ts">
import {ref, onMounted} from 'vue'
import {getUserInfo, updateUserInfo} from '../../api/user'
import {ElMessage, ElLoading, ElDialog, type FormInstance} from 'element-plus'
import {UserFilled} from '@element-plus/icons-vue'
import {uploadUserImage} from '../../api/util'

const userData = ref({
  username: '',
  name: '',
  avatar: '',
  telephone: '',
  email: '',
  location: '',
  role: '',
  password: '',
  confirmPassword: ''
})
const originalPassword = ref('')
const showReloginDialog = ref(false)
const editMode = ref(false)
const tempAvatar = ref('')
const formRef = ref<FormInstance>()
const isChangingPassword = ref(false)
const role = ref('')

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
      confirmPassword: ''
    }
    sessionStorage.setItem('role', userData.value.role);
    role.value = userData.value.role === 'user' ? "顾客" : "管理员";
    tempAvatar.value = res.data.data.avatar || ''
  } catch (error) {
    ElMessage.error('获取用户信息失败')
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

onMounted(() => {
  fetchUserInfo()
})

const handleRelogin = () => {
  sessionStorage.clear()
  window.location.href = '/login'
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
</script>

<template>
  <div class="dashboard-container">
    <el-card class="profile-card">
      <div class="avatar-section">
        <el-upload
            :auto-upload="true"
            :http-request="handleAvatarUpload"
            :show-file-list="false"
        >
          <el-avatar :size="120" :src="tempAvatar || userData.avatar">
            <template #default>
              <UserFilled style="font-size: 48px"/>
            </template>
          </el-avatar>
          <template #tip>
            <div class="upload-tip">（点击头像可更换头像）</div>
          </template>
        </el-upload>
      </div>

      <el-form
          :model="userData"
          :rules="rules"
          label-width="80px"
          class="profile-form"
          ref="formRef"
      >
        <el-form-item label="用户名">
          <el-input v-model="userData.username" disabled/>
        </el-form-item>

        <el-form-item label="身份">
          <el-input v-model="role" disabled/>
        </el-form-item>

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

        <div class="form-actions">
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
    </el-card>
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
  </div>
</template>

<style scoped>
/* 保持原有样式不变 */
.dashboard-container {
  padding: 2rem;
  background-color: #e3f6f5;
  min-height: 100vh;
}

.password-field {
  display: flex;
  gap: 10px;
  align-items: center;
}

.change-pwd-btn {
  margin-left: 10px;
  flex-shrink: 0;
}

.profile-card {
  max-width: 800px;
  margin: 0 auto;
  border-radius: 12px;
  background-color: #ffffff;
}

.avatar-section {
  text-align: center;
  padding: 2rem 0;
  border-bottom: 2px solid #bae8e8;
}

.upload-tip {
  color: #2c698d;
  font-size: 0.9rem;
  margin-top: 1rem;
}

.profile-form {
  padding: 2rem;
}

:deep(.el-form-item__label) {
  color: #2c698d;
  font-weight: 500;
}

:deep(.el-input__inner) {
  border-color: #bae8e8;
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
</style>
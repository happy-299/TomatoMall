<!-- Dashboard.vue -->
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUserInfo, updateUserInfo } from '../../api/user'
import Header from '../../components/Header.vue'
import { ElMessage, ElLoading } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'

const userData = ref({
  username: '',
  name: '',
  avatar: '',
  telephone: '',
  email: '',
  location: ''
})

const editMode = ref(false)
const avatarFile = ref<File | null>(null)
const tempAvatar = ref('')

// 模拟头像上传
const mockUpload = (file: File) => {
  return new Promise((resolve) => {
    const reader = new FileReader()
    reader.onload = () => {
      setTimeout(() => resolve(reader.result), 1000) // 模拟延迟
    }
    reader.readAsDataURL(file)
  })
}

const handleAvatarUpload = async (file: File) => {
  const loading = ElLoading.service({ fullscreen: false })
  try {
    // TODO: 实际应替换为真实上传接口
    const avatarUrl = await mockUpload(file)
    tempAvatar.value = avatarUrl as string
    ElMessage.success('头像上传成功')
  } finally {
    loading.close()
  }
}

const fetchUserInfo = async () => {
  const { username } = JSON.parse(sessionStorage.getItem('user') || '{}')
  const res = await getUserInfo(username)
  Object.assign(userData.value, res.data)
  tempAvatar.value = res.data.avatar
}

const handleSubmit = async () => {
  try {
    await updateUserInfo({
      ...userData.value,
      avatar: tempAvatar.value
    })
    ElMessage.success('信息更新成功')
    editMode.value = false
    await fetchUserInfo()
  } catch (error) {
    ElMessage.error('更新失败，请重试')
  }
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<template>
  <Header />
  <div class="dashboard-container">
    <el-card class="profile-card">
      <div class="avatar-section">
        <el-upload
            :auto-upload="false"
            :show-file-list="false"
            :on-change="(file) => handleAvatarUpload(file.raw!)"
        >
          <el-avatar :size="120" :src="tempAvatar || userData.avatar">
            <template #default>
              <UserFilled style="font-size: 48px" />
            </template>
          </el-avatar>
          <template #tip>
            <div class="upload-tip">点击更换头像（模拟上传）</div>
          </template>
        </el-upload>
      </div>

      <el-form :model="userData" label-width="80px" class="profile-form">
        <el-form-item label="用户名">
          <el-input v-model="userData.username" disabled />
        </el-form-item>

        <el-form-item label="姓名">
          <el-input v-model="userData.name" :disabled="!editMode" />
        </el-form-item>

        <el-form-item label="手机号">
          <el-input
              v-model="userData.telephone"
              :disabled="!editMode"
              placeholder="1xxxxxxxxxx"
          />
        </el-form-item>

        <el-form-item label="邮箱">
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
            <el-button @click="editMode = false">取消</el-button>
            <el-button type="primary" @click="handleSubmit">保存更改</el-button>
          </template>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.dashboard-container {
  padding: 2rem;
  background-color: #e3f6f5;
  min-height: 100vh;
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
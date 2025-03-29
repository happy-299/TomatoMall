<!-- Dashboard.vue -->
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUserInfo, updateUserInfo } from '../../api/user'
import { ElMessage, ElLoading } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { uploadUserImage } from '../../api/util'

const userData = ref({
  username: '',
  name: '',
  avatar: '',
  telephone: '',
  email: '',
  location: '',
  role: ''
})

const editMode = ref(false)
const tempAvatar = ref('')

const handleAvatarUpload = async (params: any) => { // 使用element-plus上传规范参数
  const loading = ElLoading.service({ fullscreen: false });
  try {
    const { file } = params; // 从参数中解构file对象
    console.log("upload =>", file);
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

const fetchUserInfo = async () => {
  const username = sessionStorage.getItem('username')
  if (!username) {
    ElMessage.error('未获取到用户信息，请重新登录')
    return
  }

  try {
    const res = await getUserInfo(username)
    // 更新数据映射逻辑
    userData.value = {
      username: res.data.data.username,
      name: res.data.data.name,
      avatar: res.data.data.avatar,
      telephone: res.data.data.telephone || '',
      email: res.data.data.email || '',
      location: res.data.data.location || '',
      role: res.data.data.role
    }
    tempAvatar.value = res.data.data.avatar || ''
  } catch (error) {
    ElMessage.error('获取用户信息失败')
  }
}

const handleSubmit = async () => {
  try {
    // 优化参数构造逻辑
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
  <!-- 保持template结构不变 -->
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
              <UserFilled style="font-size: 48px" />
            </template>
          </el-avatar>
          <template #tip>
            <div class="upload-tip">（点击头像可更换头像）</div>
          </template>
        </el-upload>
      </div>

      <el-form :model="userData" label-width="80px" class="profile-form">
        <el-form-item label="用户名">
          <el-input v-model="userData.username" disabled />
        </el-form-item>

        <el-form-item label="身份">
          <el-input v-model="userData.role" disabled />
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
/* 保持样式不变 */
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
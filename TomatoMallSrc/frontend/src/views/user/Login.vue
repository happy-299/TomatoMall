<script setup lang="ts">
import { ElForm, ElMessage } from "element-plus";
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { userLogin, getUserInfo, updateUserInfo } from '../../api/user.ts';

const router = useRouter();

// 输入框值
const username = ref('');
const password = ref('');

// 输入验证
const hasUsername = computed(() => username.value.trim() !== '');
const hasPassword = computed(() => password.value.trim() !== '');
const loginDisabled = computed(() => !(hasUsername.value && hasPassword.value));

// 登录处理
const handleLogin = async () => {
  try {
    const response = await userLogin({
      username: username.value,
      password: password.value
    });

    console.log('登录响应数据:', response.data);

    if (response.data.code === '200') {
      ElMessage({
        message: "登录成功！",
        type: 'success',
        center: true,
      });

      // 存储token和用户信息
      const token = response.data.data;
      sessionStorage.setItem('token', token);
      sessionStorage.setItem('username', username.value);
      
      // 获取用户信息
      try {
        const userInfoResponse = await getUserInfo(username.value);
        console.log('用户信息响应:', userInfoResponse.data);
        if (userInfoResponse.data.code === '200') {
          let userData = userInfoResponse.data.data;
          sessionStorage.setItem('userId', userData.id);
          sessionStorage.setItem('role', userData.role);
          
          // 检查是否需要赠送圣女果
          const lastLoginDate = localStorage.getItem(`lastLogin_${username.value}`);
          const today = new Date().toDateString();
          
          if (lastLoginDate !== today) {
            // 更新圣女果数量
            const newTomatoCount = (userData.tomato || 0) + 2;
            console.log('准备更新圣女果数量:', {
              username: username.value,
              currentTomato: userData.tomato,
              newTomatoCount: newTomatoCount
            });
            
            try {
              const updateResponse = await updateUserInfo({
                username: username.value,
                name: userData.name,
                avatar: userData.avatar,
                telephone: userData.telephone,
                email: userData.email,
                location: userData.location,
                role: userData.role,
                tomato: newTomatoCount
              });
              console.log('更新圣女果响应:', updateResponse.data);
              
              // 更新本地存储的最后登录日期
              localStorage.setItem(`lastLogin_${username.value}`, today);
              
              // 重新获取用户信息以更新圣女果数量
              const updatedUserInfo = await getUserInfo(username.value);
              console.log('更新后的用户信息:', updatedUserInfo.data);
              
              if (updatedUserInfo.data.code === '200') {
                userData = updatedUserInfo.data.data;
                console.log('更新后的用户数据:', userData);
              }
              
              ElMessage({
                message: "每日登录奖励：获得2个圣女果！",
                type: 'success',
                center: true,
              });
            } catch (error) {
              console.error('更新圣女果数量失败:', error);
              ElMessage.error('更新圣女果数量失败');
            }
          }
        }
      } catch (error) {
        console.error('获取用户信息失败:', error);
      }

      console.log("登录信息：", {
        token,
        username: username.value,
        userId: sessionStorage.getItem('userId'),
        role: sessionStorage.getItem('role')
      });

      // 跳转到仪表盘
      router.push({ path: "/dashboard" });
    } else {
      ElMessage({
        message: response.data.msg || '登录失败',
        type: 'error',
        center: true,
      });
      password.value = '';
    }
  } catch (error) {
    ElMessage({
      message: '请求异常，请稍后重试',
      type: 'error',
      center: true,
    });
    console.error('Login error:', error);
  }
};
</script>

<template>
  <div class="login-container">
    <el-card class="login-card">
      <h1 class="login-title">用户登录</h1>

      <el-form class="login-form">
        <el-form-item>
          <label for="username">用户名</label>
          <el-input
              id="username"
              v-model="username"
              placeholder="请输入用户名"
              :class="{ 'input-error': hasUsername && !hasUsername }"
          />
        </el-form-item>

        <el-form-item>
          <label for="password">密码</label>
          <el-input
              id="password"
              v-model="password"
              type="password"
              placeholder="••••••••"
              show-password
          />
        </el-form-item>

        <div class="action-group">
          <el-button
              class="login-btn"
              :disabled="loginDisabled"
              @click.prevent="handleLogin"
          >
            立即登录
          </el-button>

          <router-link
              to="/register"
              class="register-link"
          >
            没有账号？立即注册
          </router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(120deg, #e3f6f5 0%, #b8e1ff 100%);
}

.login-card {
  width: 420px;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(14, 31, 53, 0.1);
  background-color: #ffffff;
}

.login-title {
  color: #272643;
  text-align: center;
  margin-bottom: 32px;
  font-size: 24px;
  font-weight: 600;
}

.login-form :deep(.el-form-item__label) {
  color: #0c1d27;
  font-weight: 500;
  height: 44px;
  margin-bottom: 8px;
  display: block;
}

.login-form :deep(.el-input__inner) {
  border: none;
  height: 44px;
  border-radius: 4px;
  padding: 0 15px;
  color: #0c1d27;
  font-size: 14px;
}

.login-form :deep(.el-input__inner:focus) {
  border-color: #272643;
  box-shadow: 0 0 0 2px rgba(39, 38, 67, 0.1);
}

.input-error :deep(.el-input__inner) {
  border-color: #ff4d4f;
}

.action-group {
  margin-top: 32px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.login-btn {
  width: 100%;
  height: 44px;
  background-color: #272643;
  border-color: #272643;
  color: #ffffff;
  font-weight: 500;
  transition: all 0.3s;
}

.login-btn:hover {
  background-color: #bae8e8;
  border-color: #bae8e8;
  color: #0c1d27;
}

.login-btn:disabled {
  background-color: #e3f6f5;
  border-color: #e3f6f5;
  color: #ffffff;
  cursor: not-allowed;
}

.register-link {
  color: #272643;
  text-align: center;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s;
}

.register-link:hover {
  color: #bae8e8;
  text-decoration: underline;
}
</style>
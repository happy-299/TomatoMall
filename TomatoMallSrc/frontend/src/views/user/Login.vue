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
          sessionStorage.setItem('firstLogin', userData.firstLogin);
          console.log('firstLogin => ', userData.firstLogin);
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
              // 只发送必要的字段
              const updateResponse = await updateUserInfo({
                username: username.value,
                tomato: newTomatoCount
              });
              console.log('更新圣女果响应:', updateResponse.data);
              
              if (updateResponse.data.code === '200') {
                // 更新本地存储的最后登录日期
                localStorage.setItem(`lastLogin_${username.value}`, today);
                
                // 重新获取用户信息以更新圣女果数量
                const updatedUserInfo = await getUserInfo(username.value);
                console.log('更新后的用户信息:', updatedUserInfo.data);
                
                if (updatedUserInfo.data.code === '200') {
                  userData = updatedUserInfo.data.data;
                  console.log('更新后的用户数据:', userData);
                  
                  ElMessage({
                    message: "每日登录奖励：获得2个圣女果！",
                    type: 'success',
                    center: true,
                  });
                }
              } else {
                throw new Error(updateResponse.data.msg || '更新失败');
              }
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
      router.push({ path: "/welcome" });
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
    
    <!-- 右侧登录区域 -->
    <div class="right-section">
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

.login-container {
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
  padding: 10px;
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

/* 右侧登录区域样式 */
.right-section {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2;
  padding: 10px;
}

.login-card {
  width: 420px;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(212, 83, 83, 0.2); /* Tomato-colored shadow */
  background-color: rgba(255, 255, 255, 0.95);
  z-index: 2;
  position: relative;
  backdrop-filter: blur(10px);
}

.login-title {
  color: #d9534f; /* Tomato Red */
  text-align: center;
  margin-bottom: 32px;
  font-size: 24px;
  font-weight: 600;
  font-family: 'Arial', sans-serif;
}

.login-form :deep(.el-form-item__label) {
  color: #555;
  font-weight: 500;
  height: 44px;
  margin-bottom: 8px;
  display: block;
}

.login-form :deep(.el-input__inner) {
  border: 1px solid #ddd;
  height: 44px;
  border-radius: 4px;
  padding: 0 15px;
  color: #333;
  font-size: 14px;
  background-color: #fff;
  transition: border-color 0.3s, box-shadow 0.3s;
}

.login-form :deep(.el-input__inner:focus) {
  border-color: #d9534f;
  box-shadow: 0 0 0 2px rgba(217, 83, 79, 0.2);
}

.input-error :deep(.el-input__inner) {
  border-color: #ff4d4f;
}

/* 移除Element Plus默认的输入框样式 */
.login-form :deep(.el-input__wrapper) {
  border: none !important;
  box-shadow: none !important;
  background: transparent !important;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border: none !important;
  box-shadow: none !important;
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
  background-color: #d9534f; /* Tomato Red */
  border-color: #d9534f;
  color: #ffffff;
  font-weight: 500;
  transition: all 0.3s;
}

.login-btn:hover {
  background-color: #c9302c; /* Darker Tomato Red */
  border-color: #c12e2a;
  color: #ffffff;
}

.login-btn:disabled {
  background-color: #f8d7da; /* Light Tomato Red */
  border-color: #f8d7da;
  color: #d9534f;
  cursor: not-allowed;
}

.register-link {
  color: #28a745; /* Tomato Leaf Green */
  text-align: center;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s;
}

.register-link:hover {
  color: #218838; /* Darker Green */
  text-decoration: underline;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
  }
  
  .left-section {
    padding: 10px;
    margin-bottom: 10px;
  }
  
  .brand-title {
    font-size: 36px;
  }
  
  .brand-subtitle {
    font-size: 16px;
  }
  
  .right-section {
    padding: 10px;
  }
  
  .login-card {
    width: 100%;
    max-width: 400px;
  }
}
</style>
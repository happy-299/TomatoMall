<!-- Header.vue -->
<script setup lang="ts">
import { router } from '../router'
import { User, ShoppingCart, SwitchButton } from '@element-plus/icons-vue'

const role = sessionStorage.getItem('role')
const username = sessionStorage.getItem('username')

function logout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    customClass: 'custom-dialog',
    type: 'warning',
    center: true
  }).then(() => {
    console.log("login")
    sessionStorage.clear()
    router.push({path: "/login"})
  })
}
</script>

<template>
  <el-header class="header-container">
    <div class="header-content">
      <div class="left-section">
        <h1 class="logo-text">番茄书城</h1>
        <el-tag class="role-tag" effect="dark">{{ role === 'ADMIN' ? '管理' : '用户' }}中心</el-tag>
      </div>

      <div class="right-section">
        <el-tooltip content="购物车" placement="bottom">
          <el-icon :size="24" class="header-icon" @click="router.push('/cart')">
            <ShoppingCart />
          </el-icon>
        </el-tooltip>
        <el-tooltip content="个人中心" placement="bottom">
          <el-icon :size="24" class="header-icon" @click="router.push('/dashboard')">
            <User />
          </el-icon>
        </el-tooltip>
        <el-icon :size="24" class="header-icon" @click="logout">
          <SwitchButton />
        </el-icon>
      </div>
    </div>
  </el-header>
</template>

<style scoped>
.header-container {
  background-color: #2c698d;
  height: 60px;
  display: flex;
  align-items: center;
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30px;
}

.logo-text {
  color: #ffffff;
  margin: 0;
  font-size: 24px;
  letter-spacing: 2px;
}

.role-tag {
  margin-left: 15px;
  background-color: #bae8e8;
  color: #2c698d;
  border: none;
}

.right-section {
  display: flex;
  align-items: center;
  gap: 25px;
}

.user-info {
  display: flex;
  align-items: center;
  color: #ffffff;
  cursor: pointer;
  .username {
    margin-left: 8px;
    font-size: 14px;
  }
}

.header-icon {
  color: #ffffff;
  cursor: pointer;
  transition: color 0.3s;
  &:hover {
    color: #e3f6f5;
  }
}
</style>
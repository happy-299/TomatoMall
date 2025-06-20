<!-- Header.vue -->
<script setup lang="ts">
import { router } from '../router'
import { User, ShoppingCart, SwitchButton, Menu, Search, Tickets, Medal } from '@element-plus/icons-vue'
import { ref } from 'vue'
const searchKeyword = ref('')
const handleSearch = () => {
  if (!searchKeyword.value.trim()) return
  router.push({ path: '/search', query: { keyword: searchKeyword.value.trim() } })
}
function logout() {
  sessionStorage.clear()
  router.push({path: "/login"})
}
</script>

<template>
  <header class="header1">
    <div class="header1-content">
      <div class="header1-logo">番茄书城</div>
      <nav class="header1-nav">
        <el-tooltip content="官方认证大师榜" placement="bottom">
          <el-icon :size="24" class="header1-icon" @click="router.push('/verification-list')">
            <Medal />
          </el-icon>
        </el-tooltip>
        <el-tooltip content="优惠券" placement="bottom">
          <el-icon :size="24" class="header1-icon" @click="router.push('/coupons')">
            <Tickets />
          </el-icon>
        </el-tooltip>
        <el-tooltip content="购物车" placement="bottom">
          <el-icon :size="24" class="header1-icon" @click="router.push('/cart')">
            <ShoppingCart />
          </el-icon>
        </el-tooltip>
        <el-tooltip content="书城" placement="bottom">
          <el-icon :size="24" class="header1-icon" @click="router.push('/productlist')">
            <Menu />
          </el-icon>
        </el-tooltip>
        <el-tooltip content="个人中心" placement="bottom">
          <el-icon :size="24" class="header1-icon" @click="router.push('/dashboard')">
            <User />
          </el-icon>
        </el-tooltip>
        <el-icon :size="24" class="header1-icon" @click="logout">
          <SwitchButton />
        </el-icon>
      </nav>
      <div class="header1-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索商品、用户、书单..."
          class="header1-search-input"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon class="search-icon"><Search /></el-icon>
          </template>
        </el-input>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header1 {
  width: 100vw;
  position: fixed;
  top: 0;
  left: 0;
  z-index: 100;
  background: linear-gradient(to bottom, rgba(40,40,40,0.85) 0%, rgba(40,40,40,0.15) 100%);
  box-shadow: none;
  border: none;
  height: 72px;
  display: flex;
  align-items: center;
  backdrop-filter: blur(6px);
  opacity: 0.88;
}
.header1-content {
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 48px;
  height: 72px;
}
.header1-logo {
  color: #fff;
  font-size: 32px;
  font-weight: bold;
  letter-spacing: 2px;
  text-shadow: 0 2px 8px rgba(0,0,0,0.25);
}
.header1-nav {
  display: flex;
  gap: 32px;
  align-items: center;
}
.header1-icon {
  color: #fff;
  cursor: pointer;
  font-size: 24px;
  transition: color 0.2s;
}
.header1-icon:hover {
  color: #ffe066;
}
.header1-search {
  min-width: 320px;
  max-width: 400px;
}
.header1-search-input {
  width: 100%;
  border-radius: 24px;
  background: rgba(255,255,255,0.92);
}
.header1-search-input :deep(.el-input__wrapper) {
  border-radius: 24px;
  background: rgba(255,255,255,0.92);
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.08);
}
.search-icon {
  color: #d9534f;
  font-size: 18px;
}
</style>
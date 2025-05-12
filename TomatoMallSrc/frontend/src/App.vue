<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AiChatBall from './components/AiChatBall.vue'

const route = useRoute()
const showAiChatBall = computed(() => {
  // 根据路由名称排除Login和Register页面
  return route.name !== 'Login' && route.name !== 'Register'
})
</script>

<template>
  <el-container direction="vertical">
    <router-view />
    <AiChatBall v-if="showAiChatBall" />
  </el-container>
</template>

<style scoped>
</style>
<template>
  <el-carousel
      indicator-position="outside"
      height="300px"
      class="ad-carousel"
      :interval="5000"
      arrow="always"
  >
    <el-carousel-item v-for="ad in ads" :key="ad.id">
      <div class="ad-container" @click="goToProduct(ad.productId)">
        <img :src="ad.imgUrl" class="ad-image"/>
        <div class="ad-content">
          <h3 class="ad-title">{{ ad.title }}</h3>
          <p class="ad-description">{{ ad.content }}</p>
        </div>
      </div>
    </el-carousel-item>
  </el-carousel>
</template>
<script setup lang="ts">
import {defineProps} from 'vue'
import {useRouter} from 'vue-router'
import type {Advertisement} from '../api/advertisement'

const router = useRouter()

defineProps<{
  ads: Advertisement[]
}>()

const goToProduct = (productId: string) => {
  router.push(`/product/${productId}`)
}
</script>

<style scoped>
.ad-carousel {
  margin: 20px 0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.ad-container {
  position: relative;
  height: 100%;
  cursor: pointer;
  transition: transform 0.3s;
}

.ad-container:hover {
  transform: scale(1.02);
}

.ad-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ad-content {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 24px;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
  color: white;
}

.ad-title {
  font-size: 24px;
  margin: 0 0 12px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
}

.ad-description {
  font-size: 16px;
  margin: 0;
  line-height: 1.5;
  opacity: 0.9;
}
</style>
//D:\CollegeCourse\grade-2-spring\软工二\TomatoMall\TomatoMallSrc\frontend\src\components\AdCarousel.vue
<template>
  <el-carousel
      indicator-position="outside"
      :height="carouselHeight"
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
import {defineProps, computed} from 'vue'
import {useRouter} from 'vue-router'
import type {Advertisement} from '../api/advertisement'

const router = useRouter()

const props = defineProps<{
  ads: Advertisement[],
  height?: string
}>()

const carouselHeight = computed(() => props.height || '1000px')

const goToProduct = (productId: string) => {
  router.push(`/product/${productId}`)
}
</script>

<style scoped>
.ad-carousel {
  width: 100vw;
  height: 100%;
  margin: 0;
  padding: 0;
  border: none !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  background: none !important;
  overflow: hidden;
}

.ad-container {
  position: relative;
  width: 100%;
  height: 100%;
  cursor: pointer;
  transition: transform 0.3s;
  overflow: hidden;
}

.ad-container:hover {
  transform: scale(1.01);
}

.ad-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.ad-content {
  position: absolute;
  top: 400px;
  left: 56px;
  right: auto;
  bottom: auto;
  transform: none;
  max-width: 700px;
  width: auto;
  padding: 0;
  background: transparent;
  color: #ffffff;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-start;
  text-align: left;
}

.ad-title {
  font-size: 40px;
  margin: 0 0 18px;
  font-weight: bold;
  color: #111;
  text-shadow: none;
}

.ad-description {
  font-size: 22px;
  margin: 0;
  line-height: 1.6;
  opacity: 0.92;
  color: #222;
  text-shadow: none;
}
</style>
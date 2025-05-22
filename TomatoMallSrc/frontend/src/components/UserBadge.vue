<!-- UserBadge.vue -->
<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps({
  isVerified: {
    type: Boolean,
    required: true
  },
  verifiedName: {
    type: String,
    default: ''
  }
})

const sparkle = ref(false)

const startSparkle = () => {
  sparkle.value = true
  setTimeout(() => sparkle.value = false, 1000)
}
</script>

<template>
  <transition name="glow">
    <el-tag
        v-if="isVerified"
        :class="['badge', 'verified-badge']"
        effect="dark"
        @mouseenter="startSparkle"
    >
      <svg class="v-icon" viewBox="0 0 24 24">
        <path fill="currentColor" d="M12 25L3 12h18z"/>
      </svg>
      <span class="badge-text">{{ verifiedName }}</span>
    </el-tag>
    <el-tag v-else class="reader-badge" effect="plain">读者</el-tag>
  </transition>
</template>

<style scoped>
.badge {
  margin-left: 8px;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  height: 24px;
  line-height: 20px;
  letter-spacing: 0.5px;
  border-width: 1px;
}

.verified-badge {
  position: relative;
  background: linear-gradient(
      45deg,
      #FFD700 0%,
      #FFC800 30%,
      #D4AF37 70%,
      #FFD700 100%
  );
  border: 1px solid rgba(212, 175, 55, 0.8);
  color: #2d2d2d;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.verified-badge::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(
      45deg,
      rgba(255,255,255,0) 25%,
      rgba(255,255,255,0.8) 50%,
      rgba(255,255,255,0) 75%
  );
  animation: metal-glow 2s infinite linear;
}

.reader-badge {
  background: rgba(64,158,255,0.08);
  border-color: rgba(64,158,255,0.6);
  color: #409EFF;
  font-weight: 400;
}

.v-icon {
  width: 18px;
  height: 18px;
  margin-right: 6px;
  filter: drop-shadow(0 0 2px rgba(255,215,0,0.8));
  animation:
      icon-float 1.5s ease-in-out infinite,
      icon-glow 1s alternate infinite;
}

@keyframes metal-glow {
  0% { transform: translate(-25%, -25%) rotate(45deg); }
  100% { transform: translate(25%, 25%) rotate(45deg); }
}

@keyframes icon-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}

@keyframes icon-glow {
  from { filter: drop-shadow(0 0 2px rgba(255,215,0,0.8)); }
  to { filter: drop-shadow(0 0 5px rgba(255,215,0,0.9)); }
}

.verified-badge:hover {
  transform: scale(1.05);
  box-shadow:
      0 0 15px rgba(255,215,0,0.5),
      0 0 30px rgba(255,215,0,0.3);
}
</style>
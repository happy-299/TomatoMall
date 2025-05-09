<!-- src/components/AiChatBall.vue -->
<template>
  <!-- 添加Teleport将弹窗渲染到body根部 -->
  <Teleport to="body">
    <AiChatWindow
        v-if="showChat"
        :position="windowPosition"
        @close="showChat = false"/>
  </Teleport>

  <div class="chat-ball-wrapper">
    <div class="chat-ball"
         :style="ballStyle"
         @mousedown="startDrag"
         @touchstart.prevent="startDrag"
         @click="handleClick">
      <div class="ball">
        <span>🤖</span>
        <div class="ripple"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed} from 'vue'
import { useRouter } from 'vue-router'
import AiChatWindow from './AiChatWindow.vue'

const router = useRouter()

// 拖拽相关状态
const isDragging = ref(false)
const startX = ref(0)
const startY = ref(0)
const ballX = ref(50)
const ballY = ref(50)
const dragThreshold = 5
const ballSize = 60
const hasDragged = ref(false)

const ballStyle = ref({
  left: `${ballX.value}%`,
  top: `${ballY.value}%`,
  transform: 'translate(-50%, -50%)',
  transition: 'all 0.3s ease'
})

// 新增弹窗相关状态
const showChat = ref(false)

// 计算弹窗位置
// const windowPosition = computed(() => {
//   const ballRect = {
//     x: (ballX.value / 100) * window.innerWidth,
//     y: (ballY.value / 100) * window.innerHeight
//   }
//
//   // 保持弹窗在可视区域内
//   const maxX = window.innerWidth - 400 // 弹窗宽度+边距
//   const maxY = window.innerHeight - 500 // 弹窗高度+边距
//
//   return {
//     x: Math.min(ballRect.x + 80, maxX),
//     y: Math.min(ballRect.y - 40, maxY)
//   }
// })
const windowPosition = computed(() => {
  const ballRect = {
    x: (ballX.value / 100) * window.innerWidth,
    y: (ballY.value / 100) * window.innerHeight
  }

  return {
    x: ballRect.x + 80,
    y: ballRect.y - 40
  }
})

// 修复点1：明确定义事件处理函数
const handleClick = (e: MouseEvent) => {
  if (!hasDragged.value) {
    showChat.value = !showChat.value
  }
  hasDragged.value = false
}

// const goToChat = () => {
//   router.push('/ai-chat')
// }

// 修复点2：使用箭头函数保持上下文
const startDrag = (e: MouseEvent | TouchEvent) => {
  hasDragged.value = false
  isDragging.value = false
  const clientX = e instanceof MouseEvent ? e.clientX : e.touches[0].clientX
  const clientY = e instanceof MouseEvent ? e.clientY : e.touches[0].clientY

  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  startX.value = clientX - rect.left
  startY.value = clientY - rect.top

  // 修复点3：使用正确的事件名称
  document.addEventListener('mousemove', handleDrag)
  document.addEventListener('touchmove', handleDrag)
  document.addEventListener('mouseup', handleStopDrag)
  document.addEventListener('touchend', handleStopDrag)
}

// 修复点4：重命名处理函数
const handleDrag = (e: MouseEvent | TouchEvent) => {
  const clientX = e instanceof MouseEvent ? e.clientX : e.touches[0].clientX
  const clientY = e instanceof MouseEvent ? e.clientY : e.touches[0].clientY

  const deltaX = clientX - startX.value
  const deltaY = clientY - startY.value

  if (!isDragging.value && (
      Math.abs(deltaX) > dragThreshold ||
      Math.abs(deltaY) > dragThreshold
  )) {
    isDragging.value = true
  }

  if (isDragging.value) {
    e.preventDefault()
    const newX = (clientX / window.innerWidth) * 100
    const newY = (clientY / window.innerHeight) * 100

    ballX.value = Math.max(ballSize/2/window.innerWidth*100,
        Math.min(newX, 100 - ballSize/2/window.innerWidth*100))
    ballY.value = Math.max(ballSize/2/window.innerHeight*100,
        Math.min(newY, 100 - ballSize/2/window.innerHeight*100))

    ballStyle.value = {
      ...ballStyle.value,
      left: `${ballX.value}%`,
      top: `${ballY.value}%`,
      transition: 'none'
    }
  }
}

// 修复点5：重命名停止函数
const handleStopDrag = () => {
  document.removeEventListener('mousemove', handleDrag)
  document.removeEventListener('touchmove', handleDrag)
  document.removeEventListener('mouseup', handleStopDrag)
  document.removeEventListener('touchend', handleStopDrag)

  if (isDragging.value) {
    hasDragged.value = true
  }
  isDragging.value = false
  ballStyle.value.transition = 'all 0.3s ease'

  localStorage.setItem('chatBallPosition', JSON.stringify({
    x: ballX.value,
    y: ballY.value
  }))
}

onMounted(() => {
  const savedPos = localStorage.getItem('chatBallPosition')
  if (savedPos) {
    const { x, y } = JSON.parse(savedPos)
    ballX.value = x
    ballY.value = y
    ballStyle.value.left = `${x}%`
    ballStyle.value.top = `${y}%`
  }

  const handleResize = () => {
    ballStyle.value = {
      ...ballStyle.value,
      left: `${ballX.value}%`,
      top: `${ballY.value}%`
    }
  }

  window.addEventListener('resize', handleResize)
  onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize)
  })
})
</script>

<style scoped>
.chat-ball {
  position: fixed;
  cursor: grab;
  user-select: none;
  touch-action: none;
  z-index: 9999;
  transform: translate(-50%, -50%);
}

.chat-ball:active {
  cursor: grabbing;
}

.ball {
  width: 60px;
  height: 60px;
  background: #7c5cfc;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(124, 92, 252, 0.3);
  animation: float 3s ease-in-out infinite;
  position: relative;
}

.ripple {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 2px solid #7c5cfc;
  animation: ripple 2s infinite;
}

.chat-ball-wrapper {
  position: fixed;
  z-index: 9999;
  pointer-events: none; /* 允许穿透点击 */
}

.chat-ball {
  pointer-events: auto; /* 恢复小球点击 */
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-20px); }
}

@keyframes ripple {
  0% { transform: translate(-50%, -50%) scale(1); opacity: 1; }
  100% { transform: translate(-50%, -50%) scale(2); opacity: 0; }
}
</style>
<!-- src/components/AiChatWindow.vue -->
<template>
  <div class="ai-chat-window" :style="windowStyle">
    <div class="chat-header">
      <h3>AI导购</h3>
      <el-button
          circle
          @click="close"
          class="close-btn"
      >
        <el-icon :size="16" color="#fff">
          <Close/>
        </el-icon>
      </el-button>
    </div>

    <div class="chat-history" ref="chatScroll">
      <div v-for="msg in messages" :key="msg.id"
           :class="['message', msg.role.toLowerCase()]">
        <!-- 用户和AI消息 -->
        <div v-if="['USER', 'AI'].includes(msg.role)" class="bubble">
          {{ typeof msg.content === 'string' ? msg.content : '' }}
        </div>

        <!-- 商品卡片 -->
        <div v-else-if="msg.role === 'PRODUCT'" class="product-card">
          <div class="card-content">
            <h3 class="recommend-title">🌟 为你推荐商品</h3>
            <h4>{{ typeof msg.content === 'object' ? msg.content.title : '' }}</h4>
            <p class="price">{{ typeof msg.content === 'object' ? msg.content.price : '' }}</p>
            <el-button
                class="arrow-btn"
                @click="goToProduct(typeof msg.content === 'object' ? msg.content.id : '')"
                circle
            >
              <el-icon>
                <ArrowRight/>
              </el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="input-area">
      <el-input v-model="inputMessage"
                @keyup.enter="sendMessage"
                placeholder="输入你的问题..."></el-input>
      <el-button type="primary"
                 @click="sendMessage"
                 :loading="isLoading">发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref, onMounted, nextTick, computed, onBeforeUnmount} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import {talkToAI, postChatMessage, getSessionMessages, getChatSessions} from '../api/ai_chat.ts'
import {ChatMessage, ChatSession} from '../api/ai_chat.ts'
import {debounce} from 'lodash-es'
import {Close} from '@element-plus/icons-vue'
import {reactive} from 'vue'
import {ArrowRight} from '@element-plus/icons-vue'
import {getProductById} from '../api/product.ts'
import { ElMessage } from 'element-plus'

// Define proper types for message content
interface ProductContent {
  id: string;
  title: string;
  price: string | number;
  [key: string]: any;
}

// Extend ChatMessage to handle different content types
interface ExtendedChatMessage extends Omit<ChatMessage, 'content'> {
  content: string | ProductContent;
}

const router = useRouter()
const route = useRoute()
const messages = ref<ExtendedChatMessage[]>([])
const inputMessage = ref('')
const isLoading = ref(false)
const currentSessionId = ref('0')
const chatScroll = ref<HTMLElement>()

const goToProduct = (productId: string) => {
  // Force navigation by using replace and then push
  const targetPath = `/product/${productId}`
  
  // If we're already on a product page
  if (route.path.includes('/product/')) {
    // Use replace to force component reload
    router.replace('/productList').then(() => {
      router.push(targetPath)
    })
  } else {
    router.push(targetPath)
  }
}

const loadHistory = async () => {
  try {
    if (currentSessionId.value !== '0') {
      const res = await getSessionMessages(currentSessionId.value)
      messages.value = res.data.data.map((msg: any) => {
        if (msg.role === 'AI') {
          try {
            const parsed = JSON.parse(msg.content)
            if (parsed.reason) {
              return {...msg, content: parsed.reason, id: parsed.id}
            }
          } catch (e) {
            console.warn('解析历史消息失败:', e)
          }
        }
        return msg
      })
      console.log("Message => ",messages)
    }
  } catch (error) {
    ElMessage.error('加载历史记录失败')
  }
}

// 新增：加载会话列表并设置最新会话
const loadSessions = async () => {
  try {
    const sessionsRes = await getChatSessions()
    console.log("AI sessions => ", sessionsRes)
    if (sessionsRes.data.data && sessionsRes.data.data.length > 0) {
      const sortedSessions = sessionsRes.data.data.sort((a: ChatSession, b: ChatSession) =>
          new Date(b.updateTime).getTime() - new Date(a.updateTime).getTime()
      )
      console.log("sortedSession => ", sortedSessions)
      currentSessionId.value = sortedSessions[0].id
      await loadHistory()
    }
  } catch (error) {
    ElMessage.error('加载会话列表失败')
  }
}

// 新增滚动控制方法
const scrollToBottom = () => {
  nextTick(() => {
    if (chatScroll.value) {
      chatScroll.value.scrollTo({
        top: chatScroll.value.scrollHeight,
        behavior: 'smooth'
      })
    }
  })
}
// 重写发送消息方法
const sendMessage = async () => {
  if (!inputMessage.value.trim() || isLoading.value) return

  let tempAiMessage: ExtendedChatMessage | null = null
  try {
    isLoading.value = true
    tempAiMessage = {
      id: `temp-${Date.now()}`,
      sessionId: currentSessionId.value,
      role: 'AI' as const,
      content: '思考中...',
      createTime: new Date().toISOString()
    }

    // 添加用户消息
    messages.value.push({
      id: Date.now().toString(),
      sessionId: currentSessionId.value,
      role: 'USER' as const,
      content: inputMessage.value,
      createTime: new Date().toISOString()
    }, tempAiMessage)

    scrollToBottom()

    // 发送请求
    const response = await talkToAI({
      sessionId: currentSessionId.value,
      content: inputMessage.value
    })
    console.log("完整响应:", response)

    // 处理响应
    if (response.data.code === '200') {
      const responseData = response.data.data
      console.log("响应数据:", responseData)

      // 解析reply字段
      let parsedReply
      try {
        parsedReply = JSON.parse(responseData.reply)
        console.log("解析后的reply:", parsedReply)
      } catch (e) {
        throw new Error('解析回复内容失败')
      }

      // 创建AI消息
      const aiMessage: ExtendedChatMessage = {
        id: parsedReply.id?.toString() || `ai-${Date.now()}`,
        sessionId: responseData.sessionId?.toString() || currentSessionId.value,
        role: 'AI' as const,
        content: parsedReply.reason || '未获取到回复内容',
        createTime: new Date().toISOString()
      }

      // 更新会话ID
      if (responseData.sessionId) {
        currentSessionId.value = responseData.sessionId.toString()
      }

      // 替换临时消息
      const index = messages.value.findIndex(msg => msg.id === tempAiMessage?.id)
      if (index > -1) {
        messages.value.splice(index, 1, aiMessage)
      }

      // 保存到历史记录（取消注释）
      // await postChatMessage({
      //   sessionId: currentSessionId.value,
      //   role: 'AI',
      //   content: aiMessage.content
      // })

      // 新增商品处理
      if (parsedReply.id) {
        try {
          const productRes = await getProductById(parsedReply.id)
          const product = productRes.data.data
          console.log("Product => ", product)
          // 添加商品卡片消息（不存入历史记录）
          messages.value.push({
            id: `${product.id}`,
            sessionId: currentSessionId.value,
            role: 'PRODUCT' as const,
            content: product,
            createTime: new Date().toISOString()
          })
        } catch (error) {
          console.error('获取商品信息失败:', error)
        }
      }

    } else {
      throw new Error(response.data.msg || `请求失败，错误码：${response.data.code}`)
    }

    inputMessage.value = ''
  } catch (error: any) {
    console.error('请求出错:', error)
    ElMessage.error(error.message)

    // 清理临时消息
    if (tempAiMessage) {
      messages.value = messages.value.filter(msg => msg.id !== tempAiMessage!.id)
    }
  } finally {
    isLoading.value = false
    scrollToBottom()
  }
}


const props = defineProps({
  position: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['close'])

const close = () => {
  emit('close')
}

const windowStyle = computed(() => ({
  left: `${Math.max(20, Math.min(props.position.x, window.innerWidth - 400))}px`,
  top: `${Math.max(20, Math.min(props.position.y, window.innerHeight - 600))}px`
}))

// 添加防抖的位置更新
const debouncedUpdate = debounce(() => {
  // 强制更新样式
}, 50)

onMounted(async () => {
  window.addEventListener('resize', debouncedUpdate)
  await loadSessions() // 替换原来的加载逻辑

})

onBeforeUnmount(() => {
  window.removeEventListener('resize', debouncedUpdate)
  // debouncedUpdate.cancel() // 取消防抖
})
</script>

<style scoped>
.ai-chat-window {
  position: fixed;
  width: 360px;
  height: 70vh; /* 固定高度 */
  max-height: 600px;
  display: flex;
  flex-direction: column; /* 弹性布局 */
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(255, 71, 87, 0.2);
  backdrop-filter: blur(10px);
  z-index: 9998;
  padding: 16px;
}

.chat-history {
  flex: 1; /* 占据剩余空间 */
  overflow-y: auto; /* 启用垂直滚动 */
  padding-right: 8px; /* 为滚动条留空间 */
  margin-bottom: 16px;
}

/* 自定义滚动条样式 */
.chat-history::-webkit-scrollbar {
  width: 6px;
}

.chat-history::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
}

.chat-history::-webkit-scrollbar-thumb {
  background: rgba(255, 71, 87, 0.3);
  border-radius: 3px;
}

.bubble {
  max-width: 80%;
  padding: 12px 16px;
  word-wrap: break-word; /* 允许长单词换行 */
  white-space: pre-wrap; /* 保留换行符 */
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}


.message {
  margin: 12px 0;
  display: flex;
}

.message.user {
  justify-content: flex-end;
}

.message.ai {
  justify-content: flex-start;
}

.bubble {
  max-width: 70%;
  padding: 12px 18px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.user .bubble {
  background: #ff4757;
  color: white;
  border-radius: 18px 18px 4px 18px;
}

.ai .bubble {
  background: #f6f7ff;
  border-radius: 18px 18px 18px 4px;
}

.input-area {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.chat-header .el-button:hover {
  background: #ff3333 !important;
}

.chat-header .el-button:active {
  transform: scale(0.9);
}

.chat-header .el-button i {
  vertical-align: baseline;
}

.close-btn {
  background: #ff4d4d !important;
  border: none !important;
  padding: 8px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #ff3333 !important;
}

.close-btn:active {
  transform: scale(0.9);
}

.product-card {
  background: linear-gradient(135deg, #ff4757 0%, #ff3742 100%);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(255, 71, 87, 0.3);
  padding: 16px;
  max-width: 280px;
  margin: 16px 0;
  position: relative;
  overflow: hidden;
}

.product-card::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg,
  rgba(255, 255, 255, 0.1) 25%,
  transparent 25%,
  transparent 50%,
  rgba(255, 255, 255, 0.1) 50%,
  rgba(255, 255, 255, 0.1) 75%,
  transparent 75%);
  transform: rotate(45deg);
  animation: shine 3s infinite linear;
}

@keyframes shine {
  0% {
    transform: rotate(45deg) translateX(-50%);
  }
  100% {
    transform: rotate(45deg) translateX(50%);
  }
}

.card-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recommend-title {
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  margin: 0;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.card-content h4 {
  margin: 0;
  font-size: 16px;
  color: #fff;
  font-weight: 600;
}

.price {
  margin: 0;
  font-size: 18px;
  color: #ffd700;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 4px;
}

.price::before {
  content: '¥';
  font-size: 14px;
}

.arrow-btn {
  background: linear-gradient(45deg, #ff6b6b 0%, #ff8e53 100%);
  color: white;
  padding: 10px;
  border: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 15px rgba(255, 107, 107, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  margin-left: auto;
}

.arrow-btn:hover {
  transform: translateX(4px) scale(1.05);
  box-shadow: 0 6px 20px rgba(255, 107, 107, 0.4);
  background: linear-gradient(45deg, #ff8e53 0%, #ff6b6b 100%);
}

.arrow-btn i {
  transition: transform 0.3s;
}

.arrow-btn:hover i {
  transform: translateX(2px);
}

/* 调整消息对齐 */
.message.product {
  justify-content: flex-start;
}
</style>
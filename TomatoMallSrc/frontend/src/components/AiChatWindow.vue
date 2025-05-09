<!-- src/components/AiChatWindow.vue -->
<template>
  <div class="ai-chat-window" :style="windowStyle">
    <div class="chat-header">
      <h3>AI小助手</h3>
      <el-button
          circle
          @click="close"
          class="close-btn"
      >
        <el-icon :size="16" color="#fff">
          <Close />
        </el-icon>
      </el-button>
    </div>

    <div class="chat-history" ref="chatScroll">
      <div v-for="msg in messages" :key="msg.id"
           :class="['message', msg.role.toLowerCase()]">
        <div class="bubble">
          {{ msg.content }}
        </div>
      </div>
    </div>

    <div class="input-area">
      <el-input v-model="inputMessage"
                @keyup.enter="sendMessage"
                placeholder="输入你的问题..."></el-input>
      <el-button type="primary"
                 @click="sendMessage"
                 :loading="isLoading">发送</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick ,computed, onBeforeUnmount} from 'vue'
import { useRouter } from 'vue-router'
import { postChatMessage, getSessionMessages } from '../api/aihistory'
import { ChatMessage } from '../api/aihistory'
import { debounce } from 'lodash-es'
import { Close } from '@element-plus/icons-vue'

// API_KEY需要替换成实际的DeepSeek密钥
const API_KEY = 'your_api_key'

const router = useRouter()
const messages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const isLoading = ref(false)
const currentSessionId = ref('0')
const chatScroll = ref<HTMLElement>()


// 流式接收实现
const processStream = async (reader: ReadableStreamDefaultReader) => {
  let aiMessage = ''
  const decoder = new TextDecoder()

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    const chunk = decoder.decode(value)
    const lines = chunk.split('\n').filter(line => line.trim())

    for (const line of lines) {
      try {
        const data = JSON.parse(line.replace('data: ', ''))
        const content = data.choices[0].delta.content || ''
        aiMessage += content

        // 更新最后一条消息
        const lastMsg = messages.value[messages.value.length - 1]
        if (lastMsg.role === 'AI') {
          lastMsg.content = aiMessage
        } else {
          messages.value.push({
            id: Date.now().toString(),
            sessionId: currentSessionId.value,
            role: 'AI',
            content: aiMessage,
            createTime: new Date().toISOString()
          })
        }
      } catch (e) {
        console.error('解析错误:', e)
      }
    }
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || isLoading.value) return

  try {
    isLoading.value = true
    const userMessage = inputMessage.value

    // 添加用户消息
    messages.value.push({
      id: Date.now().toString(),
      sessionId: currentSessionId.value,
      role: 'USER',
      content: userMessage,
      createTime: new Date().toISOString()
    })

    // 保存到历史记录
    const res = await postChatMessage({
      sessionId: currentSessionId.value,
      role: 'USER',
      content: userMessage
    })

    // 更新sessionId
    if (currentSessionId.value === '0') {
      currentSessionId.value = res.data.sessionId
    }

    // 调用DeepSeek API
    const response = await fetch('https://api.deepseek.com/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${API_KEY}`
      },
      body: JSON.stringify({
        model: 'deepseek-chat',
        messages: [
          { role: 'system', content: '你是一个有帮助的助手' },
          { role: 'user', content: userMessage }
        ],
        stream: true
      })
    })

    // 处理流式响应
    const reader = response.body?.getReader()
    if (reader) {
      await processStream(reader)

      // 保存AI响应
      const lastMsg = messages.value[messages.value.length - 1]
      await postChatMessage({
        sessionId: currentSessionId.value,
        role: 'AI',
        content: lastMsg.content
      })
    }

    inputMessage.value = ''
  } finally {
    isLoading.value = false
    nextTick(() => {
      if (chatScroll.value) {
        chatScroll.value.scrollTop = chatScroll.value.scrollHeight
      }
    })
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
  // 加载历史会话
  if (currentSessionId.value !== '0') {
    const res = await getSessionMessages(currentSessionId.value)
    messages.value = res.data
  }
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
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(124, 92, 252, 0.2);
  backdrop-filter: blur(10px);
  z-index: 9998;
  padding: 16px;
  transform-origin: left top;
  transition: left 0.3s ease, top 0.3s ease;
}

.ai-chat-window::before {
  content: '';
  position: absolute;
  left: -12px;
  top: 20px;
  border: 8px solid transparent;
  border-right-color: rgba(255, 255, 255, 0.95);
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chat-history {
  height: 50vh;
  max-height: 500px;
}

.bubble {
  max-width: 80%;
  padding: 10px 16px;
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
  background: #7c5cfc;
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

</style>
<!-- src/views/AiChat.vue -->
<template>
  <div class="ai-chat-container">
    <div class="chat-header">
      <h2>AI Assistant</h2>
      <el-button @click="router.back()">返回</el-button>
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
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { postChatMessage, getSessionMessages } from '../api/ai_chat.ts'
import { ChatMessage } from '../api/ai_chat.ts'

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

onMounted(async () => {
  // 加载历史会话
  if (currentSessionId.value !== '0') {
    const res = await getSessionMessages(currentSessionId.value)
    messages.value = res.data
  }
})
</script>

<style scoped>
.ai-chat-container {
  max-width: 800px;
  margin: 20px auto;
  padding: 20px;
  border-radius: 20px;
  background: linear-gradient(145deg, #f0f2ff, #ffffff);
  box-shadow: 0 8px 24px rgba(124, 92, 252, 0.1);
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chat-history {
  height: 60vh;
  overflow-y: auto;
  padding: 15px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  border: 1px solid #eee;
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
</style>
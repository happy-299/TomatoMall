// src/api/aihistory.ts
import { axios } from '../utils/request'

export interface ChatMessage {
    id: string
    sessionId: string
    role: 'USER' | 'AI' | 'PRODUCT'
    content: string
    createTime: string
}

export interface ChatSession {
    id: string
    title: string
    createTime: string
    updateTime: string
}

// 新增处理 /api/chat/talk 的方法
export const talkToAI = (data: {
    sessionId: string
    content: string
}) => {
    const token = sessionStorage.getItem('token')
    return axios.post(`/api/chat/talk`, data, {
        headers: {
            'Content-Type': 'application/json',
            'token': token || ''
        },
    })
}

// 原方法保持不变
export const postChatMessage = (data: {
    sessionId: string
    role: 'USER' | 'AI'
    content: string
}) => {
    const token = sessionStorage.getItem('token')
    return axios.post(`/api/chat/post`, data, {
        headers: {
            'Content-Type': 'application/json',
            'token': token || ''
        }
    })
}

// 原方法保持不变
export const getChatSessions = () => {
    const token = sessionStorage.getItem('token')
    return axios.get(`/api/chat/sessions`, {
        headers: {
            'token': token || ''
        }
    })
}

// 原方法保持不变
export const getSessionMessages = (sessionId: string) => {
    const token = sessionStorage.getItem('token')
    return axios.get(`/api/chat/messages/${sessionId}`, {
        headers: {
            'token': token || ''
        }
    })
}
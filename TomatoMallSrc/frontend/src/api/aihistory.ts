// src/api/aihistory.ts
import { axios } from '../utils/request'

export interface ChatMessage {
    id: string
    sessionId: string
    role: 'USER' | 'AI'
    content: string
    createTime: string
}

export interface ChatSession {
    id: string
    title: string
    createTime: string
    updateTime: string
}

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

export const getChatSessions = () => {
    const token = sessionStorage.getItem('token')
    return axios.get(`/api/chat/sessions`, {
        headers: {
            'token': token || ''
        }
    })
}

export const getSessionMessages = (sessionId: string) => {
    const token = sessionStorage.getItem('token')
    return axios.get(`/api/messages/${sessionId}`, {
        headers: {
            'token': token || ''
        }
    })
}
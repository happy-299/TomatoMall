import { axios } from '../utils/request'
import { PRODUCT_MODULE } from "./_prefix.ts"

/**
 * 评论相关接口类型定义
 */
export interface Review {
    id: number | null
    productId: number
    userId: number | null
    rating: number
    content: string
    createTime: string | null
    images: string[]
}

export interface CreateReviewInfo {
    rating: number
    content: string
    images?: string[]
}

export interface PageResponse<T> {
    content: T[]
    totalElements: number
    totalPages: number
    size: number
    number: number
}

/**
 * 评论API接口实现
 */

// 添加商品评论
export const addReview = (productId: number, reviewInfo: CreateReviewInfo) => {
    // 从sessionStorage获取用户ID
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
        return Promise.reject(new Error('用户未登录'))
    }

    // 构造与后端ReviewVO匹配的请求体
    const requestBody = {
        rating: reviewInfo.rating,
        content: reviewInfo.content,
        images: Array.isArray(reviewInfo.images) ? [...reviewInfo.images] : [],
        userId: Number(userId)  // 添加userId字段
    }
    
    console.log('Request body:', requestBody)
    return axios.post(`${PRODUCT_MODULE}/${productId}/reviews`, requestBody, {
        headers: {
            'Content-Type': 'application/json',
            'token': sessionStorage.getItem('token')
        }
    })
}

// 获取商品评论列表
export const getReviews = (productId: number, page: number = 0, size: number = 10) => {
    return axios.get(`${PRODUCT_MODULE}/${productId}/reviews`, {
        params: {
            page,
            size
        },
        headers: {
            'token': sessionStorage.getItem('token')
        }
    })
}

// 获取商品平均评分
export const getAverageRating = (productId: number) => {
    return axios.get(`${PRODUCT_MODULE}/${productId}/rating`, {
        headers: {
            'token': sessionStorage.getItem('token')
        }
    })
} 
import { axios } from '../utils/request'
import {CART_MODULE} from "./_prefix.ts";

//购物车相关接口类型定义

export interface CartItem {
    cartItemId: string
    productId: string
    title: string
    price: number
    description?: string
    cover?: string
    detail?: string
    quantity: number
}

export interface CartListResponse {
    items: CartItem[]
    total: number
    totalAmount: number
}


//购物车API接口实现


// 添加商品到购物车
export const addToCart = (productId: string, quantity: number) => {
    const token = sessionStorage.getItem('token')
    return axios.post(CART_MODULE, { productId, quantity }, {
        headers: {
            'Content-Type': 'application/json',
            'token': token || ''
        }
    })
}

// 删除购物车商品
export const deleteCartItem = (cartItemId: string) => {
    const token = sessionStorage.getItem('token')
    return axios.delete(`${CART_MODULE}/${cartItemId}`, {
        headers: {
            'token': token || ''
        }
    })
}

// 修改购物车商品数量
export const updateCartItemQuantity = (cartItemId: string, quantity: number) => {
    const token = sessionStorage.getItem('token')
    return axios.patch(`${CART_MODULE}/${cartItemId}`, { quantity }, {
        headers: {
            'Content-Type': 'application/json',
            'token': token || ''
        }
    })
}

// 获取购物车列表
export const getCart = () => {
    const token = sessionStorage.getItem('token')
    return axios.get<CartListResponse>(CART_MODULE, {
        headers: {
            'token': token || ''
        }
    })
}
import { axios } from '../utils/request'

const PRODUCT_MODULE = '/api/products'

/**
 * 商品相关接口类型定义
 */
export interface Product {
    id: string
    title: string
    price: number
    rate: number
    description?: string
    cover?: string
    detail?: string
    specifications?: Specification[]
}

export interface Specification {
    id: string
    item: string
    value: string
    productId: string
}

export interface Stockpile {
    id: string
    productId: string
    amount: number
    frozen: number
}

export interface UpdateProductInfo {
    id: string
    title?: string
    price?: number
    rate?: number
    description?: string
    cover?: string
    detail?: string
    specifications?: Specification[]
}

export interface CreateProductInfo {
    title: string
    price: number
    rate: number
    description?: string
    cover?: string
    detail?: string
    specifications?: Specification[]
}

/**
 * 商品API接口实现
 */

// 获取商品列表
export const getProducts = () => {
    return axios.get(PRODUCT_MODULE)
}

// 获取指定商品信息
export const getProductById = (id: string) => {
    return axios.get(`${PRODUCT_MODULE}/${id}`)
}

// 更新商品信息
export const updateProduct = (productInfo: UpdateProductInfo) => {
    const token = sessionStorage.getItem('token')
    return axios.put(PRODUCT_MODULE, productInfo, {
        headers: {
            'Content-Type': 'application/json',
            'token': token || ''
        }
    })
}

// 新增商品
export const createProduct = (productInfo: CreateProductInfo) => {
    const token = sessionStorage.getItem('token')
    return axios.post(PRODUCT_MODULE, productInfo, {
        headers: {
            'Content-Type': 'application/json',
            'token': token || ''
        }
    })
}

// 删除商品
export const deleteProduct = (id: string) => {
    const token = sessionStorage.getItem('token')
    return axios.delete(`${PRODUCT_MODULE}/${id}`, {
        headers: {
            'token': token || ''
        }
    })
}

// 调整商品库存
export const adjustStockpile = (productId: string, amount: number) => {
    const token = sessionStorage.getItem('token')
    return axios.patch(`${PRODUCT_MODULE}/stockpile/${productId}`, { amount }, {
        headers: {
            'Content-Type': 'application/json',
            'token': token || ''
        }
    })
}

// 查询商品库存
export const getStockpile = (productId: string) => {
    return axios.get(`${PRODUCT_MODULE}/stockpile/${productId}`)
}
import { axios } from '../utils/request'
import { API_MODULE } from './_prefix'
import { Product } from './product'

// 书单相关接口类型定义
export interface BookListVO {
    id: number
    title: string
    creatorId: number
    description: string
    picture: string  // 添加图片字段
    products: Product[]
    creatorName: string
    creatorAvatar: string
    creationDate: string  // 使用 string 类型表示 LocalDateTime
    favouriteCount: number
}

export interface PageResponseVO<T> {
    content: T[]
    totalElements: number
    totalPages: number
    size: number
    number: number
}

// 创建书单的请求参数
export interface BookListCreateDTO {
    title: string
    description: string
    productIds: number[]
    picture: string  // 修改字段名从img为picture
}

// 收藏书单的请求参数
export interface BookListCollectDTO {
    bookListId: number
}

// 创建书单
export const createBookList = (data: BookListCreateDTO) => {
    return axios.post<BookListVO>(`${API_MODULE}/booklist`, data)
}

// 删除书单
export const deleteBookList = (id: number) => {
    return axios.delete(`${API_MODULE}/booklist/${id}`)
}

// 获取我创建的书单
export const getMyBookLists = (page: number = 0, size: number = 10) => {
    return axios.get<PageResponseVO<BookListVO>>(`${API_MODULE}/booklist/mine`, {
        params: { page, size }
    })
}

// 收藏书单
export const collectBookList = (data: BookListCollectDTO) => {
    return axios.post(`${API_MODULE}/booklist/collect`, data)
}

// 取消收藏书单
export const cancelCollectBookList = (data: BookListCollectDTO) => {
    return axios.delete(`${API_MODULE}/booklist/collect`, { data })
}

// 获取我收藏的书单
export const getFavouriteBookLists = (page: number = 0, size: number = 10) => {
    return axios.get<PageResponseVO<BookListVO>>(`${API_MODULE}/booklist/favourites`, {
        params: { page, size }
    })
}

// 向书单添加商品
export const addItemToBookList = (listId: number, productId: number) => {
    return axios.post(`${API_MODULE}/booklist/${listId}/item/${productId}`)
}

// 从书单移除商品
export const removeItemFromBookList = (listId: number, productId: number) => {
    return axios.delete(`${API_MODULE}/booklist/${listId}/item/${productId}`)
}

// 获取所有书单
export const getAllBookLists = (page: number = 0, size: number = 10) => {
    return axios.get<PageResponseVO<BookListVO>>(`${API_MODULE}/booklist/all`, {
        params: { page, size }
    })
}

// 获取指定用户的书单
export const getUserBookLists = (userId: number, page: number = 0, size: number = 10) => {
    return axios.get<PageResponseVO<BookListVO>>(`${API_MODULE}/booklist/user/${userId}`, {
        params: { page, size }
    })
} 
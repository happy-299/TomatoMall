import { axios } from '../utils/request'
import { API_MODULE } from './_prefix'
import { BookListVO } from './booklist'

// 搜索历史记录类型
export interface SearchHistoryItem {
    id: number
    keyword: string
}

// 搜索接口返回的数据类型
export interface SearchResult {
    accounts: {
        id: number
        username: string
        avatar: string
        followerCount: number
        followingCount: number
    }[]
    products: {
        id: string
        title: string
        price: number
        rate: number
        description: string
        cover: string
        detail: string
        specifications: any[] | null
    }[]
    bookLists: BookListVO[]
}

// 获取搜索历史
export const getSearchHistory = () => {
    return axios.get<SearchHistoryItem[]>(`${API_MODULE}/utils/history`)
}

// 搜索
export const search = (keyword: string) => {
    return axios.get<SearchResult>(`${API_MODULE}/utils/search`, {
        params: { keyword }
    })
} 
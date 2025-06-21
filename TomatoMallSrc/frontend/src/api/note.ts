import { axios } from '../utils/request'
import { NOTE_MODULE } from "./_prefix.ts"

export interface NoteVO {
    id: number
    title: string
    content: string
    img?: string | null
    price: number
    creatorId: number
    createTime: string
    viewCnt: number
    likeCnt: number
}

export interface CreateNoteInfo {
    title: string
    content: string
    price: number
    img?: string
}

export interface UpdateNoteInfo {
    id: number
    title?: string
    content?: string
    price?: number
    img?: string | null
}

// 创建笔记
export const createNote = (noteInfo: CreateNoteInfo) => {
    const token = sessionStorage.getItem('token')
    return axios.post(`${NOTE_MODULE}`, noteInfo, {
        headers: {
            'Content-Type': 'application/json',
            'token': token || ''
        }
    })
}

// 删除笔记
export const deleteNote = (id: number) => {
    const token = sessionStorage.getItem('token')
    return axios.delete(`${NOTE_MODULE}/${id}`, {
        headers: {
            'token': token || ''
        }
    })
}

// 更新笔记信息
export const updateNote = (noteInfo: UpdateNoteInfo) => {
    const token = sessionStorage.getItem('token')
    return axios.put(`${NOTE_MODULE}`, noteInfo, {
        headers: {
            'Content-Type': 'application/json',
            'token': token || ''
        }
    })
}

// 获取单个笔记详情
export const getNoteById = (note_id: number) => {
    const token = sessionStorage.getItem('token')
    return axios.get(`${NOTE_MODULE}/${note_id}`, {
        headers: {
            'token': token || ''
        }
    })
}

// 获取用户所有笔记
export const getUserNotes = (user_id: number) => {
    const token = sessionStorage.getItem('token')
    return axios.get(`${NOTE_MODULE}/all/${user_id}`, {
        headers: {
            'token': token || ''
        }
    })
}

// 获取全部笔记
export const getAllNotes = () => {
    const token = sessionStorage.getItem('token')
    return axios.get(`${NOTE_MODULE}/all`, {
        headers: {
            'token': token || ''
        }
    })
}

// 检查点赞状态
export const getNoteLikeStatus = (id: number) => {
    const token = sessionStorage.getItem('token')
    return axios.get<boolean>(`${NOTE_MODULE}/like/${id}`, {
        headers: {
            'token': token || ''
        }
    })
}

// 点赞笔记
export const likeNote = (id: number) => {
    const token = sessionStorage.getItem('token')
    return axios.post(`${NOTE_MODULE}/like/add/${id}`, null, {
        headers: {
            'token': token || ''
        }
    })
}

// 取消点赞笔记
export const unlikeNote = (id: number) => {
    const token = sessionStorage.getItem('token')
    return axios.post(`${NOTE_MODULE}/like/sub/${id}`, null, {
        headers: {
            'token': token || ''
        }
    })
}

// 增加浏览
export const viewNote = (id: number) => {
    const token = sessionStorage.getItem('token')
    return axios.post(`${NOTE_MODULE}/view/${id}`, null, {
        headers: {
            'token': token || ''
        }
    })
}

// 检查购买状态
export const getNotePayStatus = (id: number) => {
    const token = sessionStorage.getItem('token')
    return axios.get<boolean>(`${NOTE_MODULE}/pay/${id}`, {
        headers: {
            'token': token || ''
        }
    })
}

// 购买笔记
export const payNote = (id: number) => {
    const token = sessionStorage.getItem('token')
    return axios.post(`${NOTE_MODULE}/pay/${id}`, null, {
        headers: {
            'token': token || ''
        }
    })
}

// 获取已付费笔记
export const getPaidNotes = () => {
    const token = sessionStorage.getItem('token')
    return axios.get(`${NOTE_MODULE}/all/paid`, {
        headers: {
            'token': token || ''
        }
    })
}

// 获取已点赞笔记
export const getLikedNotes = () => {
    const token = sessionStorage.getItem('token')
    return axios.get(`${NOTE_MODULE}/all/liked`, {
        headers: {
            'token': token || ''
        }
    })
}

// 获取点赞数最高的笔记
export const getTopLikedNotes = (topNum: number = 4) => {
    const token = sessionStorage.getItem('token')
    return axios.get<NoteVO[]>(`${NOTE_MODULE}/like/top/${topNum}`, {
        headers: {
            'token': token || ''
        }
    })
}
// api/user.ts
import { axios } from '../utils/request'
import { USER_MODULE } from './_prefix'

type LoginInfo = {
    username: string
    password: string
}

type RegisterInfo = {
    username: string
    password: string
    name: string
    avatar?: string
    telephone?: string
    email?: string
    location?: string
}

type UpdateInfo = {
    username: string
    name?: string
    avatar?: string
    telephone?: string
    email?: string
    location?: string
}

// 用户登录
export const userLogin = (loginInfo: LoginInfo) => {
    return axios.post(`${USER_MODULE}/login`, loginInfo, {
        headers: { 'Content-Type': 'application/json' },
        withCredentials: true
    })
}

// 用户注册
export const userRegister = (registerInfo: RegisterInfo) => {
    return axios.post(USER_MODULE, registerInfo, {
        headers: { 'Content-Type': 'application/json' }
    })
}

// 获取用户详情
export const getUserInfo = (username: string) => {
    return axios.get(`${USER_MODULE}/${username}`, {
        withCredentials: true
    })
}

// 更新用户信息
export const updateUserInfo = (updateInfo: UpdateInfo) => {
    return axios.put(USER_MODULE, updateInfo, {
        headers: { 'Content-Type': 'application/json' },
        withCredentials: true
    })
}
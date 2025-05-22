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
    role?: string
}

type UpdateInfo = {
    username: string
    name?: string
    avatar?: string
    telephone?: string
    email?: string
    location?: string
    role?: string
    tomato?: number
}

// 用户登录
export const userLogin = (loginInfo: LoginInfo) => {
    return axios.post(`${USER_MODULE}/login`, loginInfo, {
        headers: { 'Content-Type': 'application/json' }
    })
        .then(res => {
            return res
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
    const token = sessionStorage.getItem('token');
    return axios.get(`${USER_MODULE}/${username}`, {
        headers: {
            'Content-Type': 'application/json',
            'token': token
        }
    })
}

// 更新用户信息
export const updateUserInfo = (updateInfo: UpdateInfo) => {
    const token = sessionStorage.getItem('token');
    console.log('发送更新用户信息请求:', {
        url: USER_MODULE,
        data: updateInfo,
        headers: {
            'Content-Type': 'application/json',
            'token': token
        }
    });

    // 确保所有字段都被正确传递
    const updateData = {
        ...updateInfo,
        tomato: updateInfo.tomato !== undefined ? updateInfo.tomato : undefined
    };

    return axios.put(USER_MODULE, updateData, {
        headers: {
            'Content-Type': 'application/json',
            'token': token
        }
    }).then(res => {
        console.log('更新用户信息响应:', res.data);
        return res;
    }).catch(error => {
        console.error('更新用户信息失败:', error);
        throw error;
    });
}

export const getUsersByVerifiedName = (verifiedName: string) => {
    const token = sessionStorage.getItem('token');
    return axios.post(`${USER_MODULE}/verified`, { verifiedName }, {
        headers: {
            'Content-Type': 'application/json',
            'token': token
        }
    })
}

/* 新增关注相关接口 */
// 关注用户
export const followUser = (targetId: number) => {
    const token = sessionStorage.getItem('token');
    return axios.post(`${USER_MODULE}/follow`, targetId, {
        headers: {
            'Content-Type': 'application/json',
            'token': token
        }
    })
}

// 取消关注
export const unfollowUser = (targetId: number) => {
    const token = sessionStorage.getItem('token');
    return axios.delete(`${USER_MODULE}/follow`, {
        data: targetId,
        headers: {
            'Content-Type': 'application/json',
            'token': token
        }
    })
}

// 获取关注列表（分页）
export const getFollowingList = (page?: number, size?: number) => {
    const token = sessionStorage.getItem('token');
    return axios.get(`${USER_MODULE}/following`, {
        headers: { 'token': token },
        params: { page, size }
    })
}

// 获取粉丝列表（分页）
export const getFollowersList = (page?: number, size?: number) => {
    const token = sessionStorage.getItem('token');
    return axios.get(`${USER_MODULE}/followers`, {
        headers: { 'token': token },
        params: { page, size }
    })
}
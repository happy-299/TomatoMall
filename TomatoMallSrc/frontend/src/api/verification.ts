// api/verification.ts
import { axios } from '../utils/request'
import { VERIFICATION_MODULE } from './_prefix'

/* 认证申请请求类型 */
export interface ApplyVerificationRequest {
    reasonText: string
    proofImgs: string[]
    verifiedName: string
}

/* 审核请求类型 */
export interface ReviewVerificationRequest {
    appId: number
    pass: boolean
    rejectReason?: string
}

// 提交认证申请
export const applyVerification = (request: ApplyVerificationRequest) => {
    const token = sessionStorage.getItem('token');
    return axios.post(`${VERIFICATION_MODULE}/apply`, request, {
        headers: {
            'Content-Type': 'application/json',
            'token': token
        }
    })
}

// 审核认证申请
export const reviewVerification = (request: ReviewVerificationRequest) => {
    const token = sessionStorage.getItem('token');
    return axios.post(`${VERIFICATION_MODULE}/review`, request, {
        headers: {
            'Content-Type': 'application/json',
            'token': token
        }
    })
}

// 获取我的认证记录（分页）
export const getMyVerifications = (page?: number, size?: number) => {
    const token = sessionStorage.getItem('token');
    return axios.get(`${VERIFICATION_MODULE}/mine`, {
        headers: { 'token': token },
        params: { page, size }
    })
}

// 按状态查询认证记录（分页）
export const getVerificationListByStatus = (
    status: 'APPROVED' | 'REJECTED' | 'PENDING',
    page?: number,
    size?: number
) => {
    const token = sessionStorage.getItem('token');
    return axios.get(`${VERIFICATION_MODULE}/list`, {
        headers: { 'token': token },
        params: { status, page, size }
    })
}

// 查询全部认证记录（分页）
export const getAllVerifications = (page?: number, size?: number) => {
    const token = sessionStorage.getItem('token');
    return axios.get(`${VERIFICATION_MODULE}/listall`, {
        headers: { 'token': token },
        params: { page, size }
    })
}
// api/util.ts
import { axios } from '../utils/request'
import { UTIL_MODULE } from './_prefix'

export const uploadUserImage = (file: File) => { // 直接接收File类型
    const token = sessionStorage.getItem('token');
    const formData = new FormData();
    formData.append('rawFileContent', file); // 匹配后端@RequestParam名称

    return axios.post(`${UTIL_MODULE}/upload`, formData, {
        headers: {
            'token': token // 删除手动设置的Content-Type
        }
    })
}
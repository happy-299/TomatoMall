// utils/request.ts
import axios from 'axios'

const service = axios.create({
    withCredentials: true, // 允许跨域携带cookie
    timeout: 10000
})

// 响应拦截器
service.interceptors.response.use(
    response => {
        if (response.status === 200) {
            return response.data
        }
        return Promise.reject(response)
    },
    error => {
        if (error.response) {
            switch (error.response.status) {
                case 401:
                    window.location.href = '/login'
                    break
                case 403:
                    alert('无访问权限')
                    break
            }
        }
        return Promise.reject(error)
    }
)

export { service as axios }
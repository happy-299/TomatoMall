// router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import { getUserInfo } from '../api/user'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            redirect: '/dashboard'
        },
        {
            path: '/login',
            component: () => import('../views/user/Login.vue'),
            meta: { title: '用户登录', guest: true }
        },
        {
            path: '/register',
            component: () => import('../views/user/Register.vue'),
            meta: { title: '用户注册', guest: true }
        },
        {
            path: '/dashboard',
            name: 'Dashboard',
            component: () => import('../views/user/Dashboard.vue'),
            meta: { title: '个人信息', requiresAuth: true }
        },
        {
            path: '/404',
            name: '404',
            component: () => import('../views/NotFound.vue'),
            meta: { title: '404' }
        },
        {
            path: '/:catchAll(.*)',
            redirect: '/404'
        }
    ]
})

router.beforeEach(async (to, _from, next) => {//from有意忽略，若需要使用请删除下划线
    // 设置页面标题
    if (to.meta.title) {
        document.title = `${to.meta.title} | 用户系统`
    }

    try {
        // 验证用户登录状态
        await getUserInfo('dummy') // 实际username应由后端从cookie获取
        // 已登录状态
        if (to.matched.some(record => record.meta.guest)) {
            next({ path: '/dashboard' }) // 已登录用户禁止访问登录/注册页
        } else {
            next()
        }
    } catch (e) {
        // 未登录状态
        if (to.matched.some(record => record.meta.requiresAuth)) {
            next({ path: '/login', query: { redirect: to.fullPath } })
        } else {
            next()
        }
    }
})

export { router }
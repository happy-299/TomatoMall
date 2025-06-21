import {createRouter, createWebHashHistory, createWebHistory} from "vue-router"
import { RouteRecordRaw } from "vue-router"
import SearchResult from '../views/search/SearchResult.vue'
import CouponSquare from '../views/CouponSquare.vue'
import MyCoupons from '../views/MyCoupons.vue'
import AiChat from "../views/AiChat.vue";
import FollowingList from '../views/user/FollowingList.vue'
import FollowersList from '../views/user/FollowersList.vue'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [{
        path: '/',
        redirect: '/login',
    }, {
        path: '/login',
        name: 'Login',
        component: () => import('../views/user/Login.vue'),
        meta: {title: '用户登录'}
    }, {
        path: '/register',
        name: 'Register',
        component: () => import('../views/user/Register.vue'),
        meta: {title: '用户注册'}
    }, {
        path: '/home',
        redirect: '/dashboard',
        component: () => import('../views/Home.vue'),
        children: [
            {
                path: '/dashboard',
                name: 'Dashboard',
                component: () => import('../views/user/Dashboard.vue'),
                meta: {title: '个人信息'}
            },            {
                path: '/welcome',
                name: 'Welcome',
                component: () => import('../views/user/Welcome.vue'),
                meta: {title: '欢迎导购'}
            },
             {
                path: '/productList',
                component: () => import('../views/shop/ProductList.vue'),
                meta: {title: '商品列表'}
            },{
                path: '/product/:id',
                component: () => import('../views/shop/Product.vue'),
                meta: {title: '商品详情'}
            },{
                path: '/pay',
                component: () => import('../views/shop/Pay.vue'),
                meta: {title: '支付页面'}
            },{
                path: '/cart',
                component: () => import('../views/shop/Cart.vue'),
                meta: {title: '购物车'}
            },
            {
                path: '/booklist',
                component: () => import('../views/booklist/BookList.vue'),
                meta: {title: '书单列表'}
            },
            {
                path: '/booklist/:id',
                component: () => import('../views/booklist/BookListDetail.vue'),
                meta: {title: '书单详情'}
            },
            {
                path: '/coupons',
                name: 'coupons',
                component: CouponSquare,
                meta: {title: '优惠券广场'}
            },
            {
                path: '/my-coupons',
                name: 'my-coupons',
                component: MyCoupons,
                meta: {title: '我的优惠券'}
            },
            {
                path: '/ai-chat',
                name: 'ai-chat',
                component: AiChat,
                meta: {title: '我的优惠券'}
            },
            {
                path: '/verification-list',
                name: 'VerificationList',
                component: () => import('../views/verification/VerificationList.vue'),
                meta: { title: '名家大师榜' }
            },
            {
                path: '/verification-review',
                name: 'VerificationReview',
                component: () => import('../views/verification/VerificationReview.vue'),
                meta: { title: '认证审核', permission: ['admin'] }
            },
            {
                path: '/vuser-detail/:userId',
                name: 'VUserDetail',
                component: () => import('../views/verification/VUserDetail.vue'),
                meta: { title: '用户详情' }
            },
            {
                path: '/following',
                name: 'Following',
                component: FollowingList
            },
            {
                path: '/followers',
                name: 'Followers',
                component: FollowersList
            }
        ]
    }, {
        path: '/product',
        component: () => import('../views/shop/Product.vue'),
        meta: {title: '商品详情'}
    }, {
        path: '/productList',
        component: () => import('../views/shop/ProductList.vue'),
        meta: {title: '商品列表'}
    }, {
        path: '/booklist',
        component: () => import('../views/booklist/BookList.vue'),
        meta: {title: '书单列表'}
    }, {
        path: '/404',
        name: '404',
        component: () => import('../views/NotFound.vue'),
        meta: {title: '404'}
    }, {
        path: '/search',
        name: 'SearchResult',
        component: SearchResult
    }, {
        path: '/:catchAll(.*)',
        redirect: '/404'
    }]
})

router.beforeEach((to, _, next) => {
    const token: string | null = sessionStorage.getItem('token');
    const role: string | null = sessionStorage.getItem('role')

    if (to.meta.title) {
        document.title = to.meta.title
    }

    if (token) {
        if (to.meta.permission) {
            if (to.meta.permission.includes(role!)) {
                next()
            } else {
                next('/404')
            }
        } else {
            next()
        }
    } else {
        if (to.path === '/login') {
            next();
        } else if (to.path === '/register') {
            next()
        } else {
            next('/login')
        }
    }
})

export {router}

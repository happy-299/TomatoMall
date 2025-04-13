// src/api/order.ts
import request from '../utils/request'

export interface Order {
    orderId: number; // 改为字符串类型
    username: string;
    totalAmount: number;
    paymentMethod: 'ALIPAY' | 'WECHAT';
    createTime: string;
    status: 'PENDING' | 'PAID' | 'CANCELLED'| 'FAILED';
}

export interface RetPay {
    paymentForm: string
    orderId: string  // 改为字符串类型
    totalAmount: number
    paymentMethod: string
}

export interface ShippingAddress {
    recipientName: string
    telephone: string
    postalCode: string
    province: string
    city: string
    district: string
    detailAddress: string
}
// 修正点1：参数对象语法修复
export function submitOrder(params: {
    cartItemIds: string[],
    shipping_address: {
        recipientName: string,
        telephone: string,
        zipCode: string,
        location: string // 合并的地址字符串
    },
    payment_method: 'ALIPAY' | 'WECHAT'
}): Promise<Order> {
    return request({
        url: '/api/cart/checkout',
        method: 'POST',
        data: {
            cartItemIds: params.cartItemIds,
            shipping_address: {
                recipientName: params.shipping_address.recipientName,
                telephone: params.shipping_address.telephone,
                zipCode: params.shipping_address.zipCode, // 保持字段名一致
                location: params.shipping_address.location // 直接使用合并后的字符串
            },
            payment_method: params.payment_method
        }
    }).then(response => {
        // 根据Java控制器结构调整数据访问层级
        const responseData = response.data; // 直接访问data层

        // 调试日志
        // console.log('接口原始响应:', JSON.stringify({
        //     code: response,
        //     data: responseData,
        //     message: response
        // }));

        // 转换逻辑
        const validatedData: Order = {
            orderId: responseData.data.orderId.toString(), // 强制转为字符串
            totalAmount: Number(responseData.data.totalAmount) || 0,
            username: responseData.data.username || '默认用户',
            paymentMethod: responseData.data.paymentMethod || 'ALIPAY',
            createTime: responseData.data.createTime || new Date().toISOString(),
            status: responseData.data.status || 'PENDING'
        };

        // 增强校验
        if (!/^\d+$/.test(String(validatedData.orderId))) {
            throw new Error(`订单ID格式异常: ${validatedData.orderId}`);
        }
        if (validatedData.totalAmount <= 0) {
            throw new Error(`无效金额: ${validatedData.totalAmount}`);
        }

        return validatedData;
    });
}

export function cancelOrder(orderId: number): Promise<void> {

    console.log('[取消订单] 请求参数:', {
        url: `/api/orders/${orderId}`,
        method: 'DELETE'
    });

    return request({
        url: `/api/orders/${orderId}`,
        method: 'DELETE'
    })
}


export function getOrderStatus(orderId: string): Promise<{ status: 'PENDING' | 'PAID' | 'CANCELLED' }> {
    return request({
        url: `/api/orders/${orderId}/status`,
        method: 'GET'
    }).then(response => ({
        status: response.data.status
    }));
}

// 修改支付接口响应处理
export function payOrder(orderId: string): Promise<RetPay> {
    return request({
        url: `/api/orders/${orderId}/pay`,
        method: 'POST'
    }).then(response => {
        // 调试输出完整响应结构
        console.log('支付接口原始响应:', JSON.stringify(response, null, 2))

        // 修正数据访问层级
        const responseData = response.data.data

        // 空值校验
        if (!responseData) {
            throw new Error('支付接口返回数据异常')
        }

        return {
            paymentForm: responseData.paymentForm,
            orderId: responseData.orderId.toString(), // 强制类型转换
            totalAmount: Number(responseData.totalAmount),
            paymentMethod: responseData.paymentMethod || 'ALIPAY'
        }
    })
}



export const alipayHelper = {
    renderPaymentForm(formHtml: string): void {
        const container = document.createElement('div')
        container.innerHTML = formHtml
        document.body.appendChild(container)

        const form = container.querySelector('form')
        if (form) {
            form.submit()
        } else {
            throw new Error('支付宝支付表单解析失败')
        }
    },

    handlePaymentReturn(): void {
        const urlParams = new URLSearchParams(window.location.search)
        if (urlParams.get('success') === 'true') {
            console.log('前端回调处理：支付成功')
        }
    }
}
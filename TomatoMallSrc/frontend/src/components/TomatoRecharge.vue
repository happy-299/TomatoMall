const handleSubmit = async () => {
    if (!selectedAmount.value) {
        ElMessage.warning('请选择充值金额');
        return;
    }
    if (!selectedPaymentMethod.value) {
        ElMessage.warning('请选择支付方式');
        return;
    }

    const loading = ElLoading.service({
        lock: true,
        text: '正在创建充值订单...',
        background: 'rgba(0, 0, 0, 0.7)'
    });

    try {
        console.log('提交充值数据:', {
            tomato: selectedAmount.value,
            payment_method: selectedPaymentMethod.value
        });

        const rechargeData = {
            tomato: selectedAmount.value,
            payment_method: selectedPaymentMethod.value,
            shipping_address: {
                recipientName: "系统充值",
                telephone: "00000000000",
                zipCode: "000000",
                location: "系统充值"
            }
        };

        const response = await submitTomatoRecharge(rechargeData);

        console.log('充值订单响应:', response);

        if (response.data.code === '200') {
            // 调用支付接口
            const payResponse = await payOrder(response.data.data.orderId);
            console.log('支付接口响应:', payResponse);
            
            // 保存订单ID用于后续检查
            const orderId = payResponse.orderId;
            
            // 渲染支付表单
            alipayHelper.renderPaymentForm(payResponse.paymentForm);
            
            // 关闭对话框
            emit('close');

            // 开始轮询检查支付状态
            const checkPayment = async () => {
                try {
                    const isPaid = await checkPaymentStatus(orderId);
                    console.log('支付状态检查:', { orderId, isPaid });
                    if (isPaid) {
                        ElMessage.success('充值成功！');
                        // 刷新用户信息
                        emit('recharge-success');
                        return true;
                    }
                    return false;
                } catch (error) {
                    console.error('检查支付状态失败:', error);
                    return false;
                }
            };

            // 每3秒检查一次支付状态，最多检查20次（1分钟）
            let checkCount = 0;
            const checkInterval = setInterval(async () => {
                if (checkCount >= 20) {
                    clearInterval(checkInterval);
                    return;
                }
                
                const isPaid = await checkPayment();
                if (isPaid) {
                    clearInterval(checkInterval);
                }
                
                checkCount++;
            }, 3000);
        } else {
            throw new Error(response.data.msg || '创建充值订单失败');
        }
    } catch (error) {
        console.error('充值失败:', error);
        ElMessage.error(error.message || '充值失败，请稍后重试');
    } finally {
        loading.close();
    }
};

// 监听支付成功事件
onMounted(() => {
    window.addEventListener('payment-success', () => {
        emit('recharge-success');
    });
});

onUnmounted(() => {
    window.removeEventListener('payment-success', () => {
        emit('recharge-success');
    });
}); 
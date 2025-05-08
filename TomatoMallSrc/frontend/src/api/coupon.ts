import request from '../utils/request';
import type { CouponTemplate, UserCoupon } from '../types/coupon';

// 创建优惠券模板
export function createCouponTemplate(data: Omit<CouponTemplate, 'id'>) {
  return request<CouponTemplate>({
    url: '/api/coupons/template',
    method: 'post',
    data
  });
}

// 获取所有优惠券模板
export function getAllCouponTemplates() {
  return request<CouponTemplate[]>({
    url: '/api/coupons/template/all',
    method: 'get'
  });
}

// 获取指定优惠券模板
export function getCouponTemplate(templateId: number) {
  return request<CouponTemplate>({
    url: `/api/coupons/template/${templateId}`,
    method: 'get'
  });
}

// 获取当前用户的所有优惠券
export function getUserCoupons() {
  return request<UserCoupon[]>({
    url: '/api/coupons/all',
    method: 'get'
  });
}

// 获取当前用户的指定优惠券
export function getUserCoupon(couponId: number) {
  return request<UserCoupon>({
    url: `/api/coupons/${couponId}`,
    method: 'get'
  });
}

// 领取优惠券
export function receiveCoupon(templateId: number) {
  return request<UserCoupon>({
    url: `/api/coupons/${templateId}`,
    method: 'post'
  });
}

// 检查用户是否已领取指定优惠券模板
export function checkCouponReceived(templateId: number) {
  return request<boolean>({
    url: `/api/coupons/check/${templateId}`,
    method: 'get'
  });
} 
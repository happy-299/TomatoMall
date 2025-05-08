export enum CouponType {
  FULL_REDUCTION = 'FULL_REDUCTION'
}

export interface CouponTemplate {
  id: number;
  title: string;
  description: string;
  img: string;
  type: string;
  threshold: number;
  reduce: number;
  discount: number;
  inUse: boolean;
  restCnt: number;
  expiryDateTime: string;
}

export interface UserCoupon {
  id: number;
  userId: number;
  couponTemplateId: number;
  title: string;
  description: string;
  img: string;
  type: string;
  threshold: number;
  reduce: number;
  discount: number;
  inUse: boolean;
  restCnt: number;
  expiryDateTime: string;
} 
export enum CouponType {
  FULL_REDUCTION = 'FULL_REDUCTION'
}

export interface CouponTemplate {
  id: number;
  title: string;
  description: string;
  img: string;
  type: CouponType;
  threshold: number;
  reduce: number;
  discount: number;
  inUse: boolean;
  restCnt: number;
  expiryDateTime: string;
}

export interface UserCoupon extends CouponTemplate {
  userId: number;
  couponTemplateId: number;
} 
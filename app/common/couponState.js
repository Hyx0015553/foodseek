/** 用户优惠券状态，与 fs_dict.coupon_state、后端 CouponState 一致 */
export const COUPON_STATE = {
  NORMAL: 1,
  USED: 2
}

const LABEL_TO_CODE = {
  '正常': COUPON_STATE.NORMAL,
  '已使用': COUPON_STATE.USED
}

const CODE_TO_LABEL = {}
Object.keys(LABEL_TO_CODE).forEach((k) => {
  CODE_TO_LABEL[LABEL_TO_CODE[k]] = k
})

export function couponStateCode(label) {
  if (label == null || label === '') return null
  const key = String(label).trim()
  return LABEL_TO_CODE[key] != null ? LABEL_TO_CODE[key] : null
}

export function couponStateLabel(code) {
  if (code == null) return ''
  return CODE_TO_LABEL[Number(code)] || ''
}

export function isCouponNormal(row) {
  const st = row && row.state != null ? Number(row.state) : couponStateCode(row && row.statecn)
  return st === COUPON_STATE.NORMAL
}

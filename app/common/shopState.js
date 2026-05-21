/** 店铺审核状态码，与 fs_dict.shop_audit、后端 ShopAuditState 一致 */
export const SHOP_AUDIT_STATE = {
  PENDING: 1,
  APPROVED: 2,
  REJECTED: 3
}

const LABEL_TO_CODE = {
  '待审核': SHOP_AUDIT_STATE.PENDING,
  '审核通过': SHOP_AUDIT_STATE.APPROVED,
  '审核不通过': SHOP_AUDIT_STATE.REJECTED
}

const CODE_TO_LABEL = {}
Object.keys(LABEL_TO_CODE).forEach((k) => {
  CODE_TO_LABEL[LABEL_TO_CODE[k]] = k
})

export function shopAuditCode(label) {
  if (label == null || label === '') return null
  return LABEL_TO_CODE[String(label).trim()] != null ? LABEL_TO_CODE[String(label).trim()] : null
}

export function shopAuditLabel(code) {
  if (code == null) return ''
  return CODE_TO_LABEL[Number(code)] || ''
}

export function isShopApproved(row) {
  const st = row && row.state != null ? Number(row.state) : shopAuditCode(row && row.statecn)
  return st === SHOP_AUDIT_STATE.APPROVED
}

/** 订单状态码，与 fs_dict.bill_state、后端 BillState 一致 */
export const BILL_STATE = {
  PENDING_PAY: 1,
  PAID: 2,
  SHIPPED: 3,
  COMPLETED: 4,
  REVIEWED: 5,
  CANCELLED: 6
}

const LABEL_TO_CODE = {
  '待付款': BILL_STATE.PENDING_PAY,
  '已付款': BILL_STATE.PAID,
  '已发货': BILL_STATE.SHIPPED,
  '已完成': BILL_STATE.COMPLETED,
  '已评价': BILL_STATE.REVIEWED,
  '已取消': BILL_STATE.CANCELLED
}

const CODE_TO_LABEL = {}
Object.keys(LABEL_TO_CODE).forEach((k) => {
  CODE_TO_LABEL[LABEL_TO_CODE[k]] = k
})

export function billStateCode(label) {
  if (label == null || label === '') return null
  return LABEL_TO_CODE[String(label).trim()] != null ? LABEL_TO_CODE[String(label).trim()] : null
}

export function billStateLabel(code) {
  if (code == null) return ''
  return CODE_TO_LABEL[Number(code)] || ''
}

export function isPaidBillState(row) {
  const st = row && row.state != null ? Number(row.state) : billStateCode(row && row.statecn)
  return st != null && st >= BILL_STATE.PAID && st !== BILL_STATE.CANCELLED
}

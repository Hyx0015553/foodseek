/** 探店计划状态，与 fs_dict.blogplan_state 一致 */
export const BLOGPLAN_STATE = {
  PENDING: 1,
  COMPLETED: 2,
  CANCELLED: 3
}

const LABEL_TO_CODE = {
  '待探店': BLOGPLAN_STATE.PENDING,
  '已完成': BLOGPLAN_STATE.COMPLETED,
  '已取消': BLOGPLAN_STATE.CANCELLED
}

const CODE_TO_LABEL = {}
Object.keys(LABEL_TO_CODE).forEach((k) => {
  CODE_TO_LABEL[LABEL_TO_CODE[k]] = k
})

export function blogplanStateCode(label) {
  if (label == null || label === '') return null
  const key = String(label).trim()
  return LABEL_TO_CODE[key] != null ? LABEL_TO_CODE[key] : null
}

export function blogplanStateLabel(code) {
  if (code == null) return ''
  return CODE_TO_LABEL[Number(code)] || ''
}

export function blogplanStateText(row) {
  if (!row) return ''
  if (row.statecn) return row.statecn
  const label = blogplanStateLabel(row.state)
  if (label) return label
  return typeof row.state === 'string' ? row.state : ''
}

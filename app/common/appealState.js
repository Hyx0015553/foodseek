/** 评价申诉状态，与 fs_dict.appeal_state 一致 */
export const APPEAL_STATE = {
  PENDING: 1,
  SUCCESS: 2,
  REJECTED: 3
}

const LABEL_TO_CODE = {
  '待处理': APPEAL_STATE.PENDING,
  '申诉成功': APPEAL_STATE.SUCCESS,
  '申诉失败': APPEAL_STATE.REJECTED
}

const CODE_TO_LABEL = {}
Object.keys(LABEL_TO_CODE).forEach((k) => {
  CODE_TO_LABEL[LABEL_TO_CODE[k]] = k
})

export function appealStateCode(label) {
  if (label == null || label === '') return null
  const key = String(label).trim()
  return LABEL_TO_CODE[key] != null ? LABEL_TO_CODE[key] : null
}

export function appealStateLabel(code) {
  if (code == null) return ''
  return CODE_TO_LABEL[Number(code)] || ''
}

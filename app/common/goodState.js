/** 商品上架状态，与 fs_dict.good_state、后端 GoodState 一致 */
export const GOOD_STATE = {
  ON_SHELF: 1,
  OFF_SHELF: 2
}

const LABEL_TO_CODE = {
  '上架中': GOOD_STATE.ON_SHELF,
  '已下架': GOOD_STATE.OFF_SHELF
}

const CODE_TO_LABEL = {}
Object.keys(LABEL_TO_CODE).forEach((k) => {
  CODE_TO_LABEL[LABEL_TO_CODE[k]] = k
})

export function goodStateCode(label) {
  if (label == null || label === '') return null
  return LABEL_TO_CODE[String(label).trim()] != null ? LABEL_TO_CODE[String(label).trim()] : null
}

export function goodStateLabel(code) {
  if (code == null) return ''
  return CODE_TO_LABEL[Number(code)] || ''
}

export function isGoodOnShelf(row) {
  const st = row && row.state != null ? Number(row.state) : goodStateCode(row && row.statecn)
  return st === GOOD_STATE.ON_SHELF
}

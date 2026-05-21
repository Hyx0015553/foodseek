/** 用户角色码，与 fs_dict.user_role、后端 UserRole 一致 */
export const USER_ROLE = {
  SUPER_ADMIN: 1,
  USER: 2,
  MERCHANT: 3,
  OPERATOR: 5,
  AUDITOR: 6
}

export function userRoleCode(roletype) {
  if (roletype == null || roletype === '') return null
  const n = Number(String(roletype).trim())
  return Number.isFinite(n) ? n : null
}

export function userRoleString(role) {
  if (role == null) return ''
  return String(Number(role))
}

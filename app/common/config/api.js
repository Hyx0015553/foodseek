/** 避免极少数分包/加载顺序下 uni.$u.http 尚未挂载导致整页白屏 */
function httpClient() {
	const h = uni.$u && uni.$u.http
	if (!h) {
		console.error('[api] uni.$u.http 未就绪，请确认 main.js 中已 Vue.use(uView)')
		throw new Error('HTTP未初始化')
	}
	return h
}
let rootIp = 'localhost:8088'
let varip = uni.getStorageSync('rootIp')
if(varip){
    rootIp = varip
}

export const serverUrl = 'http://'+rootIp+'/database/'
export const staticUrl = 'http://'+rootIp+'/'
export const fileUrl = 'http://'+rootIp+'/upload/'
export const uploadUrl = 'http://'+rootIp+'/database/upload'

export const listj = (data) => httpClient().get('list', data)
export const findj = (data) => httpClient().get('find', data)

/** /database/save 成功一般为纯数字 id；敏感词/审核失败返回中文错误句 */
function isDatabaseSaveFailureText(res) {
	const msg = res == null ? '' : String(res).trim()
	if (!msg) return false
	if (/^\d+$/.test(msg)) return false
	if (msg.startsWith('操作失败')) return true
	if (/敏感词|疑似违规|不当用语|请修改后再提交|请文明用语后再提交/.test(msg)) return true
	return false
}

function showSaveFailureModal(res) {
	const content = res == null ? '保存失败' : String(res).trim() || '保存失败'
	const isModeration =
		/敏感词|疑似违规|不当用语|请修改后再提交|请文明用语后再提交/.test(content)
	uni.showModal({
		title: isModeration ? '内容未通过审核' : '提交失败',
		content,
		showCancel: false,
		confirmText: '知道了'
	})
}

export const savej = (data) =>
	httpClient().get('save', data).then((res) => {
		if (isDatabaseSaveFailureText(res)) {
			showSaveFailureModal(res)
			return Promise.reject(new Error(String(res)))
		}
		return res
	})
export const deletej = (data) => httpClient().get('delete', data)
export const listSqlj = (data) => httpClient().get('listSql', data)
export const saveWxUser = (data) => httpClient().get('saveWxUser', data)
/** 获取图形验证码：{ captchaId, imageBase64 } */
export const captchaj = (data) => httpClient().get('captcha', data || {})
/** 账号密码登录（含验证码校验）：params 需 username、passwd、captchaId、captchaCode */
export const userLoginj = (data) => httpClient().get('userLogin', data)
// AI 探店文案生成
export const aiGenBlog = (data) => httpClient().get('aiGenBlog', data)
/** 多因子店铺推荐：params.uid、limit、useAi(0/1，需服务端千帆密钥) */
export const recommendShopsj = (data) => httpClient().get('recommendShops', data)

/** 商家端运营数据分析（准确销量：基于订单汇总） */
export const merchantOpsReportj = (data) => httpClient().get('merchantOpsReport', data)
/** 商家店铺评价词云 */
export const shopWordCloudj = (data) => httpClient().get('shopWordCloud', data)
/** 强制重建商家店铺评价词云 */
export const rebuildShopWordCloudj = (data) => httpClient().get('rebuildShopWordCloud', data)
/** 菜品评价词云（type=1 pid=gid） */
export const goodWordCloudj = (data) => httpClient().get('goodWordCloud', data)
/** 强制重建菜品评价词云 */
export const rebuildGoodWordCloudj = (data) => httpClient().get('rebuildGoodWordCloud', data)

/** 简单问答小助手（JSON）：params.q 必填；可选 roletype、sid、uid 用于附带事实数据 */
export const aiAssistantChatReq = (data) => httpClient().get('aiAssistantChat', data)
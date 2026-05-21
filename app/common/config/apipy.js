const http = uni.$u.http
let rootIp = '127.0.0.1:8000'
let varip = uni.getStorageSync('rootIp')
if(varip){
    rootIp = varip
}

export const serverUrl = 'http://'+rootIp+'/database/'
export const staticUrl = 'http://'+rootIp+'/static/'
export const fileUrl = 'http://'+rootIp+'/static/upload/'
export const uploadUrl = 'http://'+rootIp+'/database/upload'

export const listj = (data) => http.get('list', data)
export const findj = (data) => http.get('find', data)
export const savej = (data) => http.get('save', data)
export const deletej = (data) => http.get('delete', data)
export const listSqlj = (data) => http.get('listSql', data)
export const saveWxUser = (data) => http.get('saveWxUser', data)

const http = uni.$u.http
const rootIp = 'localhost'
const rootUrl = 'http://'+rootIp+':7001/'

export const serverUrl = 'http://'+rootIp+':7001/'
export const staticUrl = 'http://'+rootIp+':7001/static/'
export const fileUrl = 'http://'+rootIp+':7001/static/upload/'
export const uploadUrl = 'http://'+rootIp+':7001/upload'

export const listj = (data) => http.get('list', data)
export const findj = (data) => http.get('find', data)
export const savej = (data) => http.get('save', data)
export const deletej = (data) => http.get('delete', data)
export const listSqlj = (data) => http.get('listSql', data)
export const saveWxUser = (data) => http.get('saveWxUser', data)
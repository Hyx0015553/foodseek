package com.ideabobo.model;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import com.ideabobo.service.DatabaseService;
import com.ideabobo.util.BillSupport;
import com.ideabobo.util.GoodSupport;
import com.ideabobo.util.ShopSupport;
import com.ideabobo.util.UserSupport;
import com.ideabobo.util.YouhuiquanSupport;
import com.ideabobo.util.ReplayAppealSupport;
import com.ideabobo.util.ShopQaSupport;
import com.ideabobo.util.BlogplanSupport;
import com.ideabobo.util.BlogSupport;
import com.ideabobo.util.ReplaySupport;
import com.ideabobo.util.Common;
import com.ideabobo.util.GetNowTime;
import com.ideabobo.util.Page;

/**
 * 数据库工具实例类,工具原理是利用反射获取对象属性,生成增删改查的sql语句
 */
public class Dbservice {
	private DatabaseService databaseService;
	public Dbservice(DatabaseService databaseService){
		this.databaseService = databaseService;
	}

	/** 数值型外键在库中多为 varchar，若用 LIKE '%5%' 会误匹配 sid=15、25 等，统一按等值查询 */
	private static boolean isExactMatchStringField(String vname) {
		return "sid".equals(vname) || "uid".equals(vname) || "qid".equals(vname) || "state".equals(vname)
				|| "role".equals(vname) || "btype".equals(vname);
	}

	private static String escapeSqlLiteral(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("'", "''");
	}

	private static String stringWhereClause(String vname, String fv) {
		String esc = escapeSqlLiteral(fv);
		if (isExactMatchStringField(vname)) {
			return " and " + vname + " = '" + esc + "'";
		}
		return " and " + vname + " like '%" + esc + "%'";
	}
	public static String getTableName(String table){
		String pre = Common.getProperty("tableprefix");
		if (pre == null) {
			pre = "";
		}
		// 逻辑名 huihua → 物理表 fs_shop_qa_notice（与 foodseek.sql 一致；前端仍传 table=huihua）
		if ("huihua".equals(table)) {
			return pre + "shop_qa_notice";
		}
		return pre + table;
	}

	public String add(Object o,String tableName) throws Exception {
		Class c = o.getClass();
		Field[] fs = c.getDeclaredFields();
		StringBuffer sql = new StringBuffer();
		sql.append("insert into " + tableName);
		StringBuffer sql_name = new StringBuffer();
		StringBuffer sql_value = new StringBuffer();
		for (Field f : fs) {
			f.setAccessible(true);
			String fieldName = f.getName();
			if ("id".equals(fieldName)) {
			}else if("serialVersionUID".equals(fieldName)){
			} else {
				if ("String".equals(f.getType().getSimpleName())) {
					sql_name.append(fieldName.toLowerCase() + ",");
					String fd="";
					if(f.get(o)!=null){
						fd = (String) f.get(o);
					}
					sql_value.append("'" + fd + "'" + ",");
				} else if ("Double".equals(f.getType().getSimpleName())) {
					sql_name.append(f.getName().toLowerCase() + ",");
					Object ov = f.get(o);
					sql_value.append(ov == null ? "null," : ov + ",");
				} else {
					sql_name.append(f.getName().toLowerCase() + ",");
					sql_value.append(f.get(o) + ",");
				}
			}
		}
		String names = sql_name.toString().substring(0, sql_name.length() - 1);
		String values = sql_value.toString().substring(0,sql_value.length() - 1);
		sql.append("(").append(names).append(")").append(" ").append("values(").append(values).append(");");
		System.out.println(sql.toString());
		return sql.toString();
	}

	public String save(Object o, String tableName) throws Exception {
		Class<? extends Object> c = o.getClass();
		Field[] fs = c.getDeclaredFields();
		StringBuffer sql = new StringBuffer();
		sql.append("insert into ").append(tableName);
		StringBuffer sql_name = new StringBuffer();
		StringBuffer sql_value = new StringBuffer();
		boolean flag = false;
		for (Field f : fs) {
			f.setAccessible(true);
			String fieldName = f.getName();
			if ("id".equals(fieldName) && f.get(o) != null) {
				flag = true;
			}else if ("ndate".equals(fieldName) && (f.get(o) == null || "".equals(f.get(o)))) {
				f.set(o,GetNowTime.getNowTimeEn());
			}
		}
		if (flag) {
			return update(o, tableName);
		} else {
			return add(o, tableName);
		}
	}

	public String update(Object o,String tableName) throws Exception {
		Class c = o.getClass();
		Field[] fs = c.getDeclaredFields();
		StringBuffer sql = new StringBuffer();
		sql.append("update " + tableName + " set ");
		String id = "";
		for(int i=0;i<fs.length;i++){
			Field f = fs[i];
			f.setAccessible(true);
			String fieldName = f.getName();
			if ("id".equals(fieldName)) {
				id = f.get(o).toString();
			}else if("serialVersionUID".equals(fieldName)){
			}  else {
				if ("String".equals(f.getType().getSimpleName())) {
					if(f.get(o)!=null){
						sql.append(fieldName.toLowerCase()+"="+"\'" + f.get(o) + "\'" + ",");
					};
				} else {
					if(f.get(o)!=null){
						sql.append(fieldName.toLowerCase()+"=" + f.get(o)+",");
					}
				}
			}
		}
		String sql2 = sql.toString().substring(0,sql.length() - 1)+" where id="+id;
		System.out.println("update sql:"+sql2);
		return sql2;
	}

	public String delete(String tableName, Object paras) throws Exception{
		String sql = String.format("delete from %s where 1=1 ", tableName);
		if(paras!=null){
			Object o = paras;
			Class c = o.getClass();
			Field[] fields = c.getDeclaredFields();
			for(int i=0;i<fields.length;i++){
				Field field = fields[i];
				field.setAccessible(true);
				String vname = field.getName();
				if("serialVersionUID".equals(vname)){
					continue;
				}
				Object value = field.get(o);
				if(value != null){
					if(value instanceof String){
						String fv = value.toString();
						sql+=" and "+vname+" = '"+fv+"'";
					}else if(value instanceof Integer){
						int fv = (Integer)value;
						sql+=" and "+vname+"="+fv;

					}
				}
			}
		}

		return sql;
	}


	public Page getByPage(Page page,String tableName,String sort ,String order,String pageNo,String pageSize) throws Exception {
		StringBuffer sb = new StringBuffer();
		StringBuffer countsql = new StringBuffer();
		countsql.append("select count(*) from "+tableName+" where 1=1");
		sb.append("select * from "+tableName+" where 1=1");
		if(page.model!=null){
			Object o = page.model;
			Class c = o.getClass();
			Field[] fields = c.getDeclaredFields();
			for(int i=0;i<fields.length;i++){
				Field field = fields[i];
				field.setAccessible(true);
				String vname = field.getName();
				if("serialVersionUID".equals(vname)){
					continue;
				}
				Object value = field.get(page.model);
				if(value != null){
					String col = resolveQueryColumn(tableName, vname);
					if (col != null) {
						vname = col;
						value = resolveQueryValue(tableName, field.getName(), value);
					}
					if(value instanceof String){
						String fv = value.toString();
						String subSql = stringWhereClause(vname, fv);
						sb.append(subSql);
						countsql.append(subSql);
					}else if(value instanceof Integer){
						int fv = (Integer)value;
						sb.append(" and "+vname+"="+fv);
						countsql.append(" and "+vname+"="+fv);

					}
				}
			}
		}
		long totalRow = databaseService.findCount(countsql.toString());
		page.total = totalRow;
		page.count = totalRow;

		if (sort!=null){
			sb.append(" order by "+sort+" "+order);
		}
		if (pageNo!=null){
			page.pageSize = Integer.parseInt(pageSize);
			page.pageNo = Integer.parseInt(pageNo);
		}
		sb.append(" limit "+((page.pageNo-1)*(page.pageSize))+","+page.pageSize);
		List<Map<String, Object>> al = databaseService.find(sb.toString());
		page.rows = al;
		page.data = al;
		return page;
	}


	public Page getByPageSql(Page page,String sql,String sort,String order,String pageNo,String pageSize) throws Exception {

		StringBuffer sb = new StringBuffer(sql);
		String suffsql = sql.split("from")[1];
		StringBuffer countsql = new StringBuffer("select count(*) from "+suffsql);

		long totalRow = databaseService.findCount(countsql.toString());
		page.total = totalRow;
		page.count = totalRow;

		if (sort!=null){
			sb.append(" order by "+sort+" "+order);
		}
		if (pageNo!=null){
			page.pageSize = Integer.parseInt(pageSize);
			page.pageNo = Integer.parseInt(pageNo);
		}
		sb.append(" limit "+((page.pageNo-1)*(page.pageSize))+","+page.pageSize);
		List<Map<String, Object>> al = databaseService.find(sb.toString());
		page.rows = al;
		page.data = al;
		return page;
	}
	
	public Page getByPageLike(Page page,String tableName) throws Exception {
		StringBuffer sb = new StringBuffer();
		StringBuffer countsql = new StringBuffer();
		countsql.append("select count(*) from "+tableName+" where 1=1");
		sb.append("select * from "+tableName+" where 1=1");
		if(page.model!=null){
			Object o = page.model;
			Class c = o.getClass();
			Field[] fields = c.getDeclaredFields();
			for(int i=0;i<fields.length;i++){
				Field field = fields[i];
				field.setAccessible(true);
				String vname = field.getName();
				if("serialVersionUID".equals(vname)){
					continue;
				}
				Object value = field.get(page.model);
				if(value != null){
					if(value instanceof String){
						String fv = value.toString();
						String subSql = stringWhereClause(vname, fv);
						sb.append(subSql);
						countsql.append(subSql);
					}else if(value instanceof Integer){
						int fv = (Integer)value;
						sb.append(" and "+vname+" like %"+fv+"%");
						countsql.append(" and "+vname+" like %"+fv+"%");

					}
				}
			}
		}
		long totalRow = databaseService.findCount(countsql.toString());
		page.total = totalRow;

		sb.append(" limit "+((page.pageNo-1)*(page.pageSize))+","+page.pageSize);
		List<Map<String, Object>> al = databaseService.find(sb.toString());
		page.rows = al;
		return page;
	}

	private static String resolveQueryColumn(String tableName, String fieldName) {
		if (BillSupport.isBillTable(tableName)) {
			return BillSupport.resolveListFieldName(tableName, fieldName);
		}
		if (ShopSupport.isShopTable(tableName)) {
			return ShopSupport.resolveListFieldName(tableName, fieldName);
		}
		if (GoodSupport.isGoodTable(tableName)) {
			return GoodSupport.resolveListFieldName(tableName, fieldName);
		}
		if (UserSupport.isUserTable(tableName)) {
			return UserSupport.resolveListFieldName(tableName, fieldName);
		}
		if (YouhuiquanSupport.isCouponTable(tableName)) {
			return YouhuiquanSupport.resolveListFieldName(tableName, fieldName);
		}
		if (ReplayAppealSupport.isAppealTable(tableName)) {
			return ReplayAppealSupport.resolveListFieldName(tableName, fieldName);
		}
		if (ShopQaSupport.isShopQaTable(tableName)) {
			return ShopQaSupport.resolveListFieldName(tableName, fieldName);
		}
		if (BlogplanSupport.isBlogplanTable(tableName)) {
			return BlogplanSupport.resolveListFieldName(tableName, fieldName);
		}
		if (BlogSupport.isBlogTable(tableName)) {
			return BlogSupport.resolveListFieldName(tableName, fieldName);
		}
		return null;
	}

	private static Object resolveQueryValue(String tableName, String fieldName, Object value) {
		if (BillSupport.isBillTable(tableName)) {
			return BillSupport.resolveListFieldValue(tableName, fieldName, value);
		}
		if (ShopSupport.isShopTable(tableName)) {
			return ShopSupport.resolveListFieldValue(tableName, fieldName, value);
		}
		if (GoodSupport.isGoodTable(tableName)) {
			return GoodSupport.resolveListFieldValue(tableName, fieldName, value);
		}
		if (UserSupport.isUserTable(tableName)) {
			return UserSupport.resolveListFieldValue(tableName, fieldName, value);
		}
		if (YouhuiquanSupport.isCouponTable(tableName)) {
			return YouhuiquanSupport.resolveListFieldValue(tableName, fieldName, value);
		}
		if (ReplayAppealSupport.isAppealTable(tableName)) {
			return ReplayAppealSupport.resolveListFieldValue(tableName, fieldName, value);
		}
		if (ShopQaSupport.isShopQaTable(tableName)) {
			return ShopQaSupport.resolveListFieldValue(tableName, fieldName, value);
		}
		if (BlogplanSupport.isBlogplanTable(tableName)) {
			return BlogplanSupport.resolveListFieldValue(tableName, fieldName, value);
		}
		if (BlogSupport.isBlogTable(tableName)) {
			return BlogSupport.resolveListFieldValue(tableName, fieldName, value);
		}
		return value;
	}

	public String list(String tableName,Object paras,String ordersql) throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("select * from "+tableName+" where 1=1");
		if(paras!=null){
			Object o = paras;
			Class c = o.getClass();
			Field[] fields = c.getDeclaredFields();
			for(int i=0;i<fields.length;i++){
				Field field = fields[i];
				field.setAccessible(true);
				String vname = field.getName();
				if("serialVersionUID".equals(vname)){
					continue;
				}
				Object value = field.get(o);
				if(value != null){
					String col = resolveQueryColumn(tableName, vname);
					if (col != null) {
						vname = col;
						value = resolveQueryValue(tableName, field.getName(), value);
					}
					if(value instanceof String){
						String fv = value.toString();
						sb.append(stringWhereClause(vname, fv));
					}else if(value instanceof Integer){
						int fv = (Integer)value;
						sb.append(" and "+vname+"="+fv);

					}
				}
			}
		}

		if (ordersql!=null){
			sb.append(ordersql);
		}else{
			sb.append(" order by id desc");
		}



		return sb.toString();
	}

	public String list(String tableName,Object paras,boolean unLike) throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("select * from "+tableName+" where 1=1");
		if(paras!=null){
			Object o = paras;
			Class c = o.getClass();
			Field[] fields = c.getDeclaredFields();
			for(int i=0;i<fields.length;i++){
				Field field = fields[i];
				field.setAccessible(true);
				String vname = field.getName();
				if("serialVersionUID".equals(vname)){
					continue;
				}
				Object value = field.get(o);
				if(value != null){
					if(value instanceof String){
						String fv = value.toString();
						String subSql;
						if (unLike) {
							subSql = " and " + vname + " = '" + escapeSqlLiteral(fv) + "'";
						} else {
							subSql = stringWhereClause(vname, fv);
						}
						sb.append(subSql);
					}else if(value instanceof Integer){
						int fv = (Integer)value;
						sb.append(" and "+vname+"="+fv);

					}
				}
			}
		}

		sb.append(" order by id desc");
		return sb.toString();
	}

}

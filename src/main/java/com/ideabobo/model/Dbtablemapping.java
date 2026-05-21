package com.ideabobo.model;

import com.alibaba.fastjson.JSON;
import com.ideabobo.util.Common;

/**
 * 表名与实体映射。入参可为 {@link Dbservice#getTableName} 得到的带前缀物理表名，或无前缀的逻辑名（如 {@code bill}）。
 */
public class Dbtablemapping {

	private static String logicalTable(String fullOrLogical) {
		if (fullOrLogical == null || fullOrLogical.isEmpty()) {
			return "";
		}
		String pre = Common.getProperty("tableprefix");
		if (pre != null && !pre.isEmpty() && fullOrLogical.startsWith(pre)) {
			return fullOrLogical.substring(pre.length());
		}
		return fullOrLogical;
	}

	public static Object parseStringModel(String value, String table) {
		String t = logicalTable(table);
		Object object = null;
		switch (t) {
			case "bill": object = JSON.parseObject(value, Bill.class); break;
			case "blog": object = JSON.parseObject(value, Blog.class); break;
			case "blogplan": object = JSON.parseObject(value, Blogplan.class); break;
			case "btype": object = JSON.parseObject(value, Btype.class); break;
			case "good": object = JSON.parseObject(value, Good.class); break;
			case "huihua":
			case "shop_qa_notice":
				object = JSON.parseObject(value, Huihua.class);
				break;
			case "looked": object = JSON.parseObject(value, Looked.class); break;
			case "mgc": object = JSON.parseObject(value, Mgc.class); break;
			case "replay": object = JSON.parseObject(value, Replay.class); break;
			case "replay_appeal": object = JSON.parseObject(value, ReplayAppeal.class); break;
			case "shop_qa": object = JSON.parseObject(value, ShopQa.class); break;
			case "shop": object = JSON.parseObject(value, Shop.class); break;
			case "system_notify": object = JSON.parseObject(value, SystemNotify.class); break;
			case "sysmsg": object = JSON.parseObject(value, Sysmsg.class); break;
			case "type": object = JSON.parseObject(value, Type.class); break;
			case "type2": object = JSON.parseObject(value, Type2.class); break;
			case "user": object = JSON.parseObject(value, User.class); break;
			case "youhuiquan": object = JSON.parseObject(value, Youhuiquan.class); break;
		}
		return object;
	}

	public static Object getModelByTable(String table) {
		String t = logicalTable(table);
		Object object = null;
		switch (t) {
			case "bill": object = new Bill(); break;
			case "blog": object = new Blog(); break;
			case "blogplan": object = new Blogplan(); break;
			case "btype": object = new Btype(); break;
			case "good": object = new Good(); break;
			case "huihua":
			case "shop_qa_notice":
				object = new Huihua();
				break;
			case "looked": object = new Looked(); break;
			case "mgc": object = new Mgc(); break;
			case "replay": object = new Replay(); break;
			case "replay_appeal": object = new ReplayAppeal(); break;
			case "shop_qa": object = new ShopQa(); break;
			case "shop": object = new Shop(); break;
			case "system_notify": object = new SystemNotify(); break;
			case "sysmsg": object = new Sysmsg(); break;
			case "type": object = new Type(); break;
			case "type2": object = new Type2(); break;
			case "user": object = new User(); break;
			case "youhuiquan": object = new Youhuiquan(); break;
		}
		return object;
	}
}

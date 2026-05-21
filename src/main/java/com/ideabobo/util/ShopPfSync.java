package com.ideabobo.util;

import com.ideabobo.service.DatabaseService;

import java.util.List;
import java.util.Map;

/**
 * 店铺评分由「菜品评价」汇总：fs_replay.type=1 且 pid 对应 fs_good.id，按店铺下所有此类评价的平均分写回 fs_shop.pf。
 */
public final class ShopPfSync {

	private ShopPfSync() {
	}

	private static String prefix() {
		return Common.getProperty("tableprefix");
	}

	public static void refreshShopPfByGoodId(DatabaseService db, String goodIdStr) {
		if (goodIdStr == null || goodIdStr.trim().isEmpty()) {
			return;
		}
		int gid;
		try {
			gid = Integer.parseInt(goodIdStr.trim());
		} catch (NumberFormatException e) {
			return;
		}
		String pre = prefix();
		String goodT = pre + "good";
		List<Map<String, Object>> rows = db.find("SELECT sid FROM " + goodT + " WHERE id = " + gid);
		if (rows == null || rows.isEmpty()) {
			return;
		}
		Object sidObj = rows.get(0).get("sid");
		if (sidObj == null) {
			return;
		}
		try {
			int sid = Integer.parseInt(sidObj.toString().trim());
			refreshShopPfBySid(db, sid);
		} catch (NumberFormatException e) {
			// ignore
		}
	}

	public static void refreshShopPfBySid(DatabaseService db, int shopId) {
		String pre = prefix();
		String shopT = pre + "shop";
		String replayT = pre + "replay";
		String goodT = pre + "good";
		String sql = "UPDATE " + shopT + " s SET s.pf = (" +
				"SELECT ROUND(AVG(CAST(r.pf AS DECIMAL(4,2))), 1) FROM " + replayT + " r " +
				"INNER JOIN " + goodT + " g ON r.type = 1 AND r.pid = CAST(g.id AS CHAR) " +
				"WHERE g.sid = " + shopId + " AND r.pf IS NOT NULL AND TRIM(r.pf) <> ''" +
				") WHERE s.id = " + shopId;
		db.executeAction(sql);
	}
}

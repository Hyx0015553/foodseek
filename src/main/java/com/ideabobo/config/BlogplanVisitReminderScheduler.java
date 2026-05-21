package com.ideabobo.config;

import com.ideabobo.service.DatabaseService;
import com.ideabobo.util.Common;
import com.ideabobo.util.GetNowTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * 探店计划：在「计划到店日」的前第 3 个自然日，向用户 {@code fs_system_notify} 推送一条未读提醒（消息-系统-系统通知）。
 * 依赖 {@code fs_blogplan.remind_3d_sent} 防重复；仅 {@code state=1(待探店)} 的计划参与。
 */
@Component
public class BlogplanVisitReminderScheduler {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    /** 与小程序 huihua 展示、跳转约定一致 */
    private static final int MSGTYPE_BLOGPLAN_3D = 20;

    @Autowired
    private DatabaseService databaseService;

    private String tableBlogplan() {
        String pre = Common.getProperty("tableprefix");
        if (pre == null || pre.trim().isEmpty()) {
            pre = "fs_";
        }
        String base = pre.trim().replace("`", "");
        return base.endsWith("_") ? base + "blogplan" : base + "_blogplan";
    }

    private String tableSystemNotify() {
        String pre = Common.getProperty("tableprefix");
        if (pre == null || pre.trim().isEmpty()) {
            pre = "fs_";
        }
        String base = pre.trim().replace("`", "");
        return base.endsWith("_") ? base + "system_notify" : base + "_system_notify";
    }

    /**
     * 计划到店日「前第 3 个自然日」当天，每日北京时间 22:30 扫描一次。
     */
    @Scheduled(cron = "0 45 22 * * ?", zone = "Asia/Shanghai")
    public void runDailyReminders() {
        try {
            processDueReminders();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processDueReminders() {
        LocalDate today = LocalDate.now(ZONE);
        String tPlan = "`" + tableBlogplan() + "`";
        String sql = "SELECT id, uid, sid, stitle, plantime FROM " + tPlan
                + " WHERE state = 1 AND (remind_3d_sent IS NULL OR remind_3d_sent = 0)"
                + " AND plantime IS NOT NULL AND TRIM(plantime) <> ''";
        List<Map<String, Object>> rows = databaseService.find(sql);
        if (rows == null || rows.isEmpty()) {
            return;
        }
        // 与订单/会话 ndate 一致，用 yyyy-MM-dd HH:mm:ss，避免 getNowTime() 中文年月日在部分链路编码异常
        String ndate = GetNowTime.getNowTimeEn();
        String tNotify = "`" + tableSystemNotify() + "`";
        for (Map<String, Object> row : rows) {
            Object idObj = row.get("id");
            if (idObj == null) {
                continue;
            }
            int planId = parseInt(idObj, 0);
            if (planId <= 0) {
                continue;
            }
            Object uidObj = row.get("uid");
            if (uidObj == null) {
                continue;
            }
            String uid = String.valueOf(uidObj).trim();
            if (uid.isEmpty() || "null".equalsIgnoreCase(uid)) {
                continue;
            }
            String plantime = row.get("plantime") == null ? "" : String.valueOf(row.get("plantime")).trim();
            LocalDate planDate = parsePlanLocalDate(plantime);
            if (planDate == null) {
                continue;
            }
            if (planDate.isBefore(today)) {
                continue;
            }
            if (!today.equals(planDate.minusDays(3))) {
                continue;
            }
            String stitle = row.get("stitle") == null ? "" : String.valueOf(row.get("stitle")).trim();
            Object sidObj = row.get("sid");
            String sidStr = sidObj == null ? "" : String.valueOf(sidObj).trim();
            String note = buildNote(stitle, plantime, planId, sidStr);
            String title = "探店计划提醒";
            String ins = "INSERT INTO " + tNotify
                    + " (`uid`,`title`,`note`,`ndate`,`type`,`msgtype`) VALUES ('"
                    + escSql(uid) + "','" + escSql(title) + "','" + escSql(note) + "','"
                    + escSql(ndate) + "',1," + MSGTYPE_BLOGPLAN_3D + ")";
            try {
                databaseService.executeAction(ins);
                String upd = "UPDATE " + tPlan + " SET remind_3d_sent=1 WHERE id=" + planId;
                databaseService.executeAction(upd);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static int parseInt(Object o, int def) {
        if (o == null) {
            return def;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static String escSql(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("'", "''");
    }

    /**
     * 正文末行 {@code __REF:id=...,sid=...__} 供小程序解析跳转；展示时由前端去掉。
     */
    private static String buildNote(String stitle, String plantime, int planId, String sidStr) {
        String shopPart = stitle.isEmpty() ? "该店铺" : "「" + stitle + "」";
        String sidPart = sidStr == null ? "" : sidStr.trim();
        return "您计划在" + shopPart + "的探店时间为 " + plantime
                + "，距今还有 3 天，请提前安排出行与时间。\n__REF:id=" + planId + ",sid=" + sidPart + "__";
    }

    private static LocalDate parsePlanLocalDate(String plantime) {
        if (plantime == null || plantime.trim().isEmpty()) {
            return null;
        }
        String s = plantime.trim();
        // 先取前 10 位日期（兼容「分钟只有一位」等非常规时间串，工具里列宽截断显示也容易误判）
        if (s.length() >= 10 && s.charAt(4) == '-' && s.charAt(7) == '-') {
            try {
                return LocalDate.parse(s.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ignored) {
            }
        }
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
                "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM/dd"};
        for (String p : patterns) {
            try {
                if (p.contains("H")) {
                    DateTimeFormatter f = DateTimeFormatter.ofPattern(p);
                    LocalDateTime dt = LocalDateTime.parse(s, f);
                    return dt.toLocalDate();
                }
                return LocalDate.parse(s, DateTimeFormatter.ofPattern(p));
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }
}

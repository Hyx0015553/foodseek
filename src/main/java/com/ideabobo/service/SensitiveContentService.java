package com.ideabobo.service;

import cn.hutool.json.JSONObject;
import com.ideabobo.util.Common;
import com.ideabobo.util.GetNowTime;
import com.ideabobo.util.QianfanChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用保存前的敏感内容审核（词库 + 可选 AI 兜底）。
 */
@Service
public class SensitiveContentService {

    private static final Logger LOG = Logger.getLogger(SensitiveContentService.class.getName());

    private static final long WORD_CACHE_TTL_MS = 60 * 1000;
    private static final Pattern RISK_JSON_PATTERN = Pattern.compile("\"risk\"\\s*:\\s*(\\d+)");
    private static final Pattern REASON_JSON_PATTERN = Pattern.compile("\"reason\"\\s*:\\s*\"([^\"]*)\"");
    /** 本地兜底：高频粗口/辱骂片段（子串匹配），避免千帆漏判或未配置密钥时静默放行 */
    private static final String[] BUILTIN_COARSE_FRAGMENTS = {
            "卧槽", "我操", "我艹", "操你", "操死", "艹", "傻逼", "傻比", "煞笔", "沙雕", "狗日", "狗日的",
            "尼玛", "你妈逼", "妈逼", "nmsl", "NMSL", "草泥马", "贱人", "婊子",
            "fuck", "shit", "bitch", "dick", "pussy", "asshole"
    };

    /** 整段文本很短且与列表完全一致时拦截（避免「你妈」子串误杀「带你妈妈来」等） */
    private static final int BUILTIN_EXACT_WHOLE_MAX_LEN = 16;
    private static final Set<String> BUILTIN_EXACT_WHOLE_TEXT = new HashSet<String>();

    static {
        String[] whole = {
                "你妈", "尼玛", "傻逼", "煞笔", "傻比", "废物", "滚", "爬", "有病", "去死",
                "cnm", "nmsl", "sb", "rnm", "wdnmd"
        };
        for (String w : whole) {
            if (w == null || w.isEmpty()) {
                continue;
            }
            BUILTIN_EXACT_WHOLE_TEXT.add(w);
            if (w.matches("^[a-zA-Z0-9]+$")) {
                BUILTIN_EXACT_WHOLE_TEXT.add(w.toLowerCase(Locale.ROOT));
            }
        }
    }

    @Autowired
    private DatabaseService databaseService;

    private volatile List<String> cachedWords = Collections.emptyList();
    private volatile long cachedAt = 0L;
    private volatile boolean auditTablesEnsured = false;

    public static final class CheckResult {
        public final boolean pass;
        public final String message;
        public final List<String> hitWords;
        public final boolean aiBlocked;

        public CheckResult(boolean pass, String message, List<String> hitWords, boolean aiBlocked) {
            this.pass = pass;
            this.message = message;
            this.hitWords = hitWords == null ? Collections.<String>emptyList() : hitWords;
            this.aiBlocked = aiBlocked;
        }
    }

    public CheckResult checkBeforeSave(String tableReq, Object model) {
        if (!isEnabled()) {
            return new CheckResult(true, "", Collections.<String>emptyList(), false);
        }
        String table = normalizeTableName(tableReq);
        if (table.isEmpty() || "mgc".equals(table) || model == null) {
            return new CheckResult(true, "", Collections.<String>emptyList(), false);
        }

        List<String> fields = resolveCheckFields(table);
        if (fields.isEmpty()) {
            return new CheckResult(true, "", Collections.<String>emptyList(), false);
        }

        String text = collectModelText(model, fields);
        if (text.trim().isEmpty()) {
            return new CheckResult(true, "", Collections.<String>emptyList(), false);
        }

        List<String> hitWords = hitWords(text);
        if (!hitWords.isEmpty()) {
            logBlocked(table, fields, text, "word", hitWords, "词库命中");
            return new CheckResult(false, "操作失败：内容包含敏感词，请修改后再提交", hitWords, false);
        }

        boolean aiFirst = isAiBeforeBuiltin();
        if (aiFirst) {
            CheckResult aiBlock = tryAiModeration(table, fields, text);
            if (aiBlock != null) {
                return aiBlock;
            }
        }

        if (isBuiltinCoarseEnabled()) {
            String coarse = firstBuiltinCoarseHit(text);
            if (coarse != null) {
                List<String> coarseHits = Collections.singletonList(coarse);
                logBlocked(table, fields, text, "builtin", coarseHits, "不当用语");
                return new CheckResult(false, "操作失败：内容包含不当用语，请文明用语后再提交", coarseHits, false);
            }
        }

        if (!aiFirst) {
            CheckResult aiBlock = tryAiModeration(table, fields, text);
            if (aiBlock != null) {
                return aiBlock;
            }
        }

        return new CheckResult(true, "", Collections.<String>emptyList(), false);
    }

    /**
     * 千帆判定拦截时返回非 null；未开启或未拦截返回 null。
     */
    private CheckResult tryAiModeration(String table, List<String> fields, String text) {
        if (!isAiEnabled()) {
            return null;
        }
        AiModerationResult aiResult = aiModerate(text, table, fields);
        if (aiResult.blocked) {
            logBlocked(table, fields, text, "ai", Collections.<String>emptyList(), aiResult.reason);
            saveAiCandidate(table, fields, text, aiResult.reason);
            return new CheckResult(false, "操作失败：内容疑似违规，请修改后再提交", Collections.<String>emptyList(), true);
        }
        return null;
    }

    public void onTableMutated(String tableReqOrTableName) {
        String table = normalizeTableName(tableReqOrTableName);
        if ("mgc".equals(table)) {
            clearWordCache();
        }
    }

    private boolean isEnabled() {
        String raw = Common.getProperty("sensitive_check_enabled");
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        return !("0".equals(v) || "false".equals(v) || "off".equals(v));
    }

    private boolean isAiEnabled() {
        String raw = Common.getProperty("sensitive_ai_enabled");
        if (raw == null || raw.trim().isEmpty()) {
            return false;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        return "1".equals(v) || "true".equals(v) || "on".equals(v);
    }

    /** true：词库通过后先千帆再本地粗口（易体现 AI 拦截与候选）；false：先粗口再千帆（与历史行为一致） */
    private boolean isAiBeforeBuiltin() {
        String raw = Common.getProperty("sensitive_ai_before_builtin");
        if (raw == null || raw.trim().isEmpty()) {
            return false;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        return "1".equals(v) || "true".equals(v) || "on".equals(v);
    }

    private boolean isAiDebug() {
        String raw = Common.getProperty("sensitive_ai_debug");
        if (raw == null || raw.trim().isEmpty()) {
            return false;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        return "1".equals(v) || "true".equals(v) || "on".equals(v);
    }

    /** 未配置则默认开启，与词库互补 */
    private boolean isBuiltinCoarseEnabled() {
        String raw = Common.getProperty("sensitive_builtin_coarse_enabled");
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        return !("0".equals(v) || "false".equals(v) || "off".equals(v));
    }

    private static String firstBuiltinCoarseHit(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String trimmed = text.trim();
        if (!trimmed.isEmpty() && trimmed.length() <= BUILTIN_EXACT_WHOLE_MAX_LEN) {
            if (BUILTIN_EXACT_WHOLE_TEXT.contains(trimmed)) {
                return trimmed;
            }
            String trimmedAsciiLower = trimmed.toLowerCase(Locale.ROOT);
            if (BUILTIN_EXACT_WHOLE_TEXT.contains(trimmedAsciiLower)) {
                return trimmedAsciiLower;
            }
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String frag : BUILTIN_COARSE_FRAGMENTS) {
            if (frag == null || frag.isEmpty()) {
                continue;
            }
            if (frag.length() == 1) {
                if (text.contains(frag)) {
                    return frag;
                }
                continue;
            }
            if (frag.matches("^[a-z]+$")) {
                if (lower.contains(frag)) {
                    return frag;
                }
            } else {
                if (text.contains(frag)) {
                    return frag;
                }
            }
        }
        return null;
    }

    private boolean isAuditEnabled() {
        String raw = Common.getProperty("sensitive_audit_enabled");
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        return !("0".equals(v) || "false".equals(v) || "off".equals(v));
    }

    private boolean isAiCandidateEnabled() {
        String raw = Common.getProperty("sensitive_ai_candidate_enabled");
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        return !("0".equals(v) || "false".equals(v) || "off".equals(v));
    }

    private String normalizeTableName(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) {
            return "";
        }
        String pre = Common.getProperty("tableprefix");
        if (pre != null && pre.trim().length() > 0) {
            String p = pre.trim().toLowerCase(Locale.ROOT);
            if (t.startsWith(p)) {
                t = t.substring(p.length());
            }
        }
        return t;
    }

    private Map<String, List<String>> parseCheckRuleMap() {
        String ruleRaw = Common.getProperty("sensitive_check_rules");
        if (ruleRaw == null || ruleRaw.trim().isEmpty()) {
            ruleRaw = "blog:title,note;replay:note;shop_qa:note;huihua:note,qtitle";
        }
        Map<String, List<String>> ruleMap = new HashMap<String, List<String>>();
        String[] rows = ruleRaw.split(";");
        for (String row : rows) {
            if (row == null) {
                continue;
            }
            String item = row.trim();
            if (item.isEmpty()) {
                continue;
            }
            String[] kv = item.split(":", 2);
            String table = normalizeTableName(kv[0]);
            if (table.isEmpty()) {
                continue;
            }
            List<String> fields = new ArrayList<String>();
            if (kv.length > 1) {
                for (String f : kv[1].split(",")) {
                    if (f != null) {
                        String one = f.trim().toLowerCase(Locale.ROOT);
                        if (!one.isEmpty()) {
                            fields.add(one);
                        }
                    }
                }
            }
            if (!fields.isEmpty()) {
                ruleMap.put(table, fields);
            }
        }
        return ruleMap;
    }

    private List<String> resolveCheckFields(String table) {
        Map<String, List<String>> map = parseCheckRuleMap();
        List<String> fields = map.get(table);
        if (fields == null) {
            return Collections.emptyList();
        }
        return fields;
    }

    private String collectModelText(Object model, List<String> fields) {
        if (model == null || fields == null || fields.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Class<?> c = model.getClass();
        for (String fname : fields) {
            if (fname == null || fname.isEmpty()) {
                continue;
            }
            Field f = null;
            try {
                f = c.getDeclaredField(fname);
                f.setAccessible(true);
                Object val = f.get(model);
                if (val != null) {
                    String s = val.toString().trim();
                    if (!s.isEmpty()) {
                        if (sb.length() > 0) {
                            sb.append('\n');
                        }
                        sb.append(s);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return sb.toString();
    }

    private List<String> loadWords() {
        long now = System.currentTimeMillis();
        if (now - cachedAt < WORD_CACHE_TTL_MS && cachedWords != null && !cachedWords.isEmpty()) {
            return cachedWords;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (now - cachedAt < WORD_CACHE_TTL_MS && cachedWords != null && !cachedWords.isEmpty()) {
                return cachedWords;
            }
            List<Map<String, Object>> rows = databaseService.find("select title from fs_mgc where title is not null and title<>''");
            LinkedHashSet<String> unique = new LinkedHashSet<String>();
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    if (row == null) {
                        continue;
                    }
                    Object t = row.get("title");
                    if (t == null) {
                        continue;
                    }
                    String w = t.toString().trim();
                    if (!w.isEmpty()) {
                        unique.add(w);
                    }
                }
            }
            cachedWords = new ArrayList<String>(unique);
            cachedAt = now;
            return cachedWords;
        }
    }

    private void clearWordCache() {
        cachedWords = Collections.emptyList();
        cachedAt = 0L;
    }

    private List<String> hitWords(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> words = loadWords();
        if (words == null || words.isEmpty()) {
            return Collections.emptyList();
        }
        String lowerText = text.toLowerCase(Locale.ROOT);
        List<String> hits = new ArrayList<String>();
        for (String w : words) {
            if (w == null) {
                continue;
            }
            String t = w.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (lowerText.contains(t.toLowerCase(Locale.ROOT))) {
                hits.add(t);
                if (hits.size() >= 3) {
                    break;
                }
            }
        }
        return hits;
    }

    private AiModerationResult aiModerate(String text, String table, List<String> fields) {
        try {
            String safeText = text;
            if (safeText.length() > 1200) {
                safeText = safeText.substring(0, 1200);
            }
            String prompt = "你是面向小程序/外卖场景的严格内容审核员。请判断下列文本是否应拦截（risk=1）。\n"
                    + "【必须判 risk=1】违法违规、色情低俗、涉政极端、诈骗诱导；人身攻击、仇恨辱骂；脏话粗口与常见谐音/变体"
                    + "（如卧槽、我操、艹、傻逼、NMSL、狗日、尼玛等）；单独或仅有「你妈」「尼玛」等辱骂用语（即使很短）；性暗示与猥亵表达。\n"
                    + "【判 risk=0】单纯口味/份量/上菜慢等业务中性差评，且无脏话辱骂。\n"
                    + "只输出一行合法 JSON，不要代码块、不要解释："
                    + "{\"risk\":0或1,\"reason\":\"不超过30字\"}\n"
                    + "业务表：" + table + "\n"
                    + "字段：" + Arrays.toString(fields.toArray()) + "\n"
                    + "待审文本：\n" + safeText;
            String ai = QianfanChatClient.chat(
                    "你只输出 JSON。宁可误伤带脏字的玩笑，也不放行公开场景的粗口与辱骂。", prompt);
            return parseAiModerationResponse(ai);
        } catch (Exception ex) {
            if (isAiDebug()) {
                LOG.log(Level.WARNING, "敏感词千帆审核异常，已降级放行: " + ex.getMessage(), ex);
            }
            return new AiModerationResult(false, "");
        }
    }

    /** 从模型回复中尽量抽出 risk；兼容 ```json 包裹、中文标点等 */
    private static AiModerationResult parseAiModerationResponse(String ai) {
        if (ai == null || ai.trim().isEmpty()) {
            return new AiModerationResult(false, "");
        }
        String raw = ai.trim();
        String jsonSlice = extractJsonObjectSlice(raw);
        if (jsonSlice != null && !jsonSlice.isEmpty()) {
            try {
                JSONObject jo = new JSONObject(jsonSlice);
                Object r = jo.get("risk");
                if (r != null) {
                    int rv = r instanceof Number ? ((Number) r).intValue() : Integer.parseInt(r.toString().trim());
                    String reason = jo.getStr("reason");
                    if (reason == null) {
                        reason = "";
                    }
                    return new AiModerationResult(rv >= 1, reason);
                }
            } catch (Exception ignored) {
            }
        }
        Matcher m = RISK_JSON_PATTERN.matcher(raw);
        String reason = "";
        Matcher rm = REASON_JSON_PATTERN.matcher(raw);
        if (rm.find()) {
            reason = rm.group(1);
        }
        if (m.find()) {
            return new AiModerationResult("1".equals(m.group(1)), reason);
        }
        String t = raw.toLowerCase(Locale.ROOT);
        boolean blocked = t.contains("\"risk\":1") || t.contains("\"risk\": 1") || t.contains("'risk':1")
                || t.contains("风险:1") || t.contains("风险为1") || t.contains("risk=1");
        return new AiModerationResult(blocked, reason);
    }

    private static String extractJsonObjectSlice(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) {
                t = t.substring(firstNl + 1);
            }
            int fence = t.indexOf("```");
            if (fence > 0) {
                t = t.substring(0, fence).trim();
            }
        }
        int b = t.indexOf('{');
        int e = t.lastIndexOf('}');
        if (b >= 0 && e > b) {
            return t.substring(b, e + 1);
        }
        return t;
    }

    private static final class AiModerationResult {
        final boolean blocked;
        final String reason;

        AiModerationResult(boolean blocked, String reason) {
            this.blocked = blocked;
            this.reason = reason == null ? "" : reason;
        }
    }

    private void ensureAuditTables() {
        if (auditTablesEnsured) {
            return;
        }
        synchronized (this) {
            if (auditTablesEnsured) {
                return;
            }
            String pre = Common.getProperty("tableprefix");
            if (pre == null || pre.trim().isEmpty()) {
                pre = "fs_";
            }
            String logTable = pre + "sensitive_hit_log";
            String candidateTable = pre + "sensitive_ai_candidate";
            databaseService.executeAction(
                    "create table if not exists " + logTable + " (" +
                            "id int primary key auto_increment," +
                            "tablename varchar(64)," +
                            "fieldnames varchar(128)," +
                            "checkmode varchar(32)," +
                            "hitwords varchar(128)," +
                            "reason varchar(256)," +
                            "content text," +
                            "ndate datetime" +
                            ")"
            );
            databaseService.executeAction(
                    "create table if not exists " + candidateTable + " (" +
                            "id int primary key auto_increment," +
                            "tablename varchar(64)," +
                            "fieldnames varchar(128)," +
                            "reason varchar(256)," +
                            "content text," +
                            "state tinyint not null default 1," +
                            "ndate datetime" +
                            ")"
            );
            auditTablesEnsured = true;
        }
    }

    private void logBlocked(String table, List<String> fields, String text, String mode, List<String> hitWords, String reason) {
        if (!isAuditEnabled()) {
            return;
        }
        try {
            ensureAuditTables();
            String pre = Common.getProperty("tableprefix");
            if (pre == null || pre.trim().isEmpty()) {
                pre = "fs_";
            }
            String logTable = pre + "sensitive_hit_log";
            String content = text == null ? "" : text;
            if (content.length() > 1000) {
                content = content.substring(0, 1000);
            }
            String sql = "insert into " + logTable + "(tablename,fieldnames,checkmode,hitwords,reason,content,ndate) values(" +
                    "'" + esc(table) + "'," +
                    "'" + esc(String.join(",", fields)) + "'," +
                    "'" + esc(mode) + "'," +
                    "'" + esc(String.join(",", hitWords == null ? Collections.<String>emptyList() : hitWords)) + "'," +
                    "'" + esc(reason == null ? "" : reason) + "'," +
                    "'" + esc(content) + "'," +
                    "'" + esc(GetNowTime.getNowTimeEn()) + "')";
            databaseService.executeAction(sql);
        } catch (Exception ignored) {
        }
    }

    private void saveAiCandidate(String table, List<String> fields, String text, String reason) {
        if (!isAiCandidateEnabled()) {
            return;
        }
        try {
            ensureAuditTables();
            String pre = Common.getProperty("tableprefix");
            if (pre == null || pre.trim().isEmpty()) {
                pre = "fs_";
            }
            String candidateTable = pre + "sensitive_ai_candidate";
            String content = text == null ? "" : text;
            if (content.length() > 1000) {
                content = content.substring(0, 1000);
            }
            String sql = "insert into " + candidateTable + "(tablename,fieldnames,reason,content,state,ndate) values(" +
                    "'" + esc(table) + "'," +
                    "'" + esc(String.join(",", fields)) + "'," +
                    "'" + esc(reason == null ? "" : reason) + "'," +
                    "'" + esc(content) + "'," +
                    "1," +
                    "'" + esc(GetNowTime.getNowTimeEn()) + "')";
            databaseService.executeAction(sql);
        } catch (Exception ignored) {
        }
    }

    private String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("'", "''");
    }
}

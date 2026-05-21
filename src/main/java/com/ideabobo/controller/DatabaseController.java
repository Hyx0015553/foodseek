package com.ideabobo.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.alibaba.fastjson.JSON;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import com.ideabobo.constant.AppealState;
import com.ideabobo.constant.BillState;
import com.ideabobo.constant.BlogplanState;
import com.ideabobo.constant.ShopAuditState;
import com.ideabobo.model.*;
import com.ideabobo.service.DatabaseService;
import com.ideabobo.service.SensitiveContentService;
import com.ideabobo.util.*;
import com.ideabobo.util.wxlogin.WxLoginUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.util.UriEncoder;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@CrossOrigin(origins="*")
@Controller
@RequestMapping(value = "/database")
public class DatabaseController {
    //private static final Logger logger = Logger.getLogger(DatabaseController.class);
    @Autowired
	private DatabaseService databaseService;
	@Autowired
	private SensitiveContentService sensitiveContentService;

	/** 千帆运营简报/店铺建议短时缓存，降低重复点击成本（facts 仍每次实时计算） */
	private static final long AI_OPS_CACHE_TTL_MS = 5 * 60 * 1000;
	private static final ConcurrentHashMap<String, AiOpsCacheEntry> AI_OPS_TEXT_CACHE = new ConcurrentHashMap<>();
	private static final long WORDCLOUD_CACHE_TTL_MS = 10 * 60 * 1000;
	private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
	private static final Pattern URL_PATTERN = Pattern.compile("(https?://|www\\.)\\S+");
	private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");
	private static final Pattern NON_WORD_PATTERN = Pattern.compile("[^\\u4e00-\\u9fa5A-Za-z0-9 ]");
	private static final Pattern PURE_DIGITS_PATTERN = Pattern.compile("^\\d+$");
	private static final Set<String> BUILTIN_WORDCLOUD_STOP_WORDS = new HashSet<>();
	private static final Set<String> LOW_QUALITY_WORD_PARTS = new HashSet<>();

	static {
		Collections.addAll(BUILTIN_WORDCLOUD_STOP_WORDS, "的", "了", "是", "很", "也", "和", "就", "都", "还", "又", "与", "及",
				"在", "有", "我", "你", "他", "她", "它", "我们", "你们", "他们", "这家", "这家店", "店铺", "商家", "餐厅", "真的", "感觉", "就是",
				"可以", "不错", "一般", "一个", "这个", "那个", "不会", "没有",
				"丰富", "适合", "效率", "继续", "下单", "整体", "体验",
				// 外卖/点餐模板与流程词（非菜品口味本身，易占词云）
				"选择", "时段", "部分", "全部", "订单", "配送", "送达", "取餐", "自取", "堂食", "打包", "备注", "默认", "系统",
				"支付", "付款", "退款", "优惠", "活动", "客服", "电话", "地址", "地图", "导航", "距离", "公里", "分钟", "小时",
				"速度", "准时", "迟到", "提前", "排队", "等位", "预约", "取消", "确认", "提交", "操作", "点击", "打开", "关闭",
				"如果", "但是", "因为", "所以", "然后", "最后", "另外", "以及", "或者", "还有", "什么", "怎么", "如何", "为什么",
				"非常", "比较", "有点", "还算", "算是", "基本", "完全", "可能", "应该", "还是", "已经", "正在", "需要", "希望",
				"建议", "意见", "反馈", "投诉", "申请", "处理", "结果", "原因", "今天", "明天", "昨天", "刚才", "一次", "每次");
		// 极端噪声词可写死在 LOW_QUALITY_WORD_PARTS，或维护在表 fs_word_stop
	}

	/** 评价词云分词：优先结巴（SEARCH 模式），失败时回退到旧版窗口切分 */
	private static final JiebaSegmenter WORDCLOUD_JIEBA;
	static {
		JiebaSegmenter seg = null;
		try {
			seg = new JiebaSegmenter();
		} catch (Throwable ignored) {
		}
		WORDCLOUD_JIEBA = seg;
	}

	private static final class AiOpsCacheEntry {
		final String text;
		final long expireAt;

		AiOpsCacheEntry(String text, long expireAt) {
			this.text = text;
			this.expireAt = expireAt;
		}
	}

	private static String aiOpsCacheGet(String key) {
		AiOpsCacheEntry e = AI_OPS_TEXT_CACHE.get(key);
		if (e == null) {
			return null;
		}
		if (System.currentTimeMillis() > e.expireAt) {
			AI_OPS_TEXT_CACHE.remove(key, e);
			return null;
		}
		return e.text;
	}

	private static void aiOpsCachePut(String key, String text) {
		if (key == null || text == null) {
			return;
		}
		AI_OPS_TEXT_CACHE.put(key, new AiOpsCacheEntry(text, System.currentTimeMillis() + AI_OPS_CACHE_TTL_MS));
	}

	private static boolean isPaidBillState(Map<String, Object> bill) {
		if (bill == null) {
			return false;
		}
		Object st = bill.get("state");
		if (st != null) {
			return BillState.isPaidOrLater(BillSupport.toInteger(st));
		}
		Object statecn = bill.get("statecn");
		return BillState.isPaidOrLaterLabel(statecn == null ? null : statecn.toString());
	}

	private static void applyTableQueryAliases(String table, Object model, HttpServletRequest req) {
		if (model instanceof Shop) {
			ShopSupport.applyListRequest((Shop) model, req);
		}
		if (model instanceof Good) {
			GoodSupport.applyListRequest((Good) model, req);
		}
		if (model instanceof User) {
			UserSupport.applyListRequest((User) model, req);
		}
		if (model instanceof Youhuiquan) {
			YouhuiquanSupport.applyListRequest((Youhuiquan) model, req);
		}
		if (model instanceof ReplayAppeal) {
			ReplayAppealSupport.applyListRequest((ReplayAppeal) model, req);
		}
		if (model instanceof ShopQa) {
			ShopQaSupport.applyListRequest((ShopQa) model, req);
		}
		if (model instanceof Blogplan) {
			BlogplanSupport.applyListRequest((Blogplan) model, req);
		}
		if (model instanceof Blog) {
			BlogSupport.applyListRequest((Blog) model, req);
		}
		if (model instanceof Replay) {
			ReplaySupport.applyListRequest((Replay) model, req);
		}
	}

	private void enrichSqlRows(String sql, List<Map<String, Object>> rows) {
		if (rows == null || sql == null) {
			return;
		}
		String sqlLower = sql.toLowerCase();
		if (sqlLower.contains("youhuiquan")) {
			YouhuiquanSupport.enrichRows(rows);
		}
		if (sqlLower.contains("replay_appeal")) {
			ReplayAppealSupport.enrichRows(rows);
		}
		if (sqlLower.contains("shop_qa") && !sqlLower.contains("shop_qa_notice")) {
			ShopQaSupport.enrichRows(rows);
		}
		if (sqlLower.contains("blogplan")) {
			BlogplanSupport.enrichRows(rows);
		}
		if (sqlLower.contains("blog") && !sqlLower.contains("blogplan")) {
			BlogSupport.enrichRows(rows);
		}
		if (sqlLower.contains("replay") && !sqlLower.contains("replay_appeal")) {
			ReplaySupport.enrichRows(rows);
		}
		if (sqlLower.contains("bill")) {
			BillSupport.enrichRows(rows);
		}
		if (sqlLower.contains("good") && !sqlLower.contains("wordcloud")) {
			GoodSupport.enrichRows(rows);
		}
		if (sqlLower.contains("shop") && !sqlLower.contains("shop_qa")) {
			ShopSupport.enrichRows(rows);
		}
		if (sqlLower.contains("fs_user") && !sqlLower.contains("user_favorite")) {
			UserSupport.enrichRows(databaseService, rows);
		}
	}

	private void enrichTableRows(String table, List<Map<String, Object>> rows) {
		if (rows == null) {
			return;
		}
		if (BillSupport.isBillTable(table)) {
			BillSupport.enrichRows(rows);
		}
		if (ShopSupport.isShopTable(table)) {
			ShopSupport.enrichRows(rows);
		}
		if (GoodSupport.isGoodTable(table)) {
			GoodSupport.enrichRows(rows);
		}
		if (UserSupport.isUserTable(table)) {
			UserSupport.enrichRows(databaseService, rows);
		}
		if (YouhuiquanSupport.isCouponTable(table)) {
			YouhuiquanSupport.enrichRows(rows);
		}
		if (ReplayAppealSupport.isAppealTable(table)) {
			ReplayAppealSupport.enrichRows(rows);
		}
		if (ShopQaSupport.isShopQaTable(table)) {
			ShopQaSupport.enrichRows(rows);
		}
		if (BlogplanSupport.isBlogplanTable(table)) {
			BlogplanSupport.enrichRows(rows);
		}
		if (BlogSupport.isBlogTable(table)) {
			BlogSupport.enrichRows(rows);
		}
		if (ReplaySupport.isReplayTable(table)) {
			ReplaySupport.enrichRows(rows);
		}
	}

	private static int parseIntSafe(String raw, int def) {
		try {
			if (raw == null) {
				return def;
			}
			String t = raw.trim();
			if (t.isEmpty()) {
				return def;
			}
			return Integer.parseInt(t);
		} catch (Exception e) {
			return def;
		}
	}

	private static double parseDoubleSafe(String raw, double def) {
		try {
			if (raw == null) {
				return def;
			}
			String t = raw.trim();
			if (t.isEmpty()) {
				return def;
			}
			return Double.parseDouble(t);
		} catch (Exception e) {
			return def;
		}
	}

	private static List<String> splitCsv(String csv) {
		if (csv == null) {
			return Collections.emptyList();
		}
		String t = csv.trim();
		if (t.isEmpty()) {
			return Collections.emptyList();
		}
		String[] parts = t.split("\\s*,\\s*");
		List<String> out = new ArrayList<>();
		for (String p : parts) {
			if (p != null) {
				String x = p.trim();
				if (!x.isEmpty()) {
					out.add(x);
				}
			}
		}
		return out;
	}

	/**
	 * 解析订单明细数量（与 gids 对齐）：从 gnames 解析末尾 "*N"（如 "宜宾炒饭*2"），否则为 1。
	 */
	private static List<Integer> parseBillCounts(String gidsCsv, String gnamesCsv) {
		List<String> gids = splitCsv(gidsCsv);
		int n = gids.size();
		if (n == 0) {
			return Collections.emptyList();
		}
		List<Integer> counts = new ArrayList<>(Collections.nCopies(n, 1));

		List<String> gnames = splitCsv(gnamesCsv);
		if (!gnames.isEmpty()) {
			Pattern p = Pattern.compile("\\*(\\d+)\\s*$");
			for (int i = 0; i < Math.min(n, gnames.size()); i++) {
				String name = gnames.get(i);
				if (name == null) {
					continue;
				}
				Matcher m = p.matcher(name.trim());
				if (m.find()) {
					int v = parseIntSafe(m.group(1), 1);
					if (v < 1) {
						v = 1;
					}
					counts.set(i, v);
				}
			}
		}
		return counts;
	}

	private static String dayOfNdate(Object ndateObj) {
		if (ndateObj == null) {
			return "";
		}
		String s = ndateObj.toString().trim();
		if (s.length() >= 10) {
			return s.substring(0, 10);
		}
		return s;
	}

	/**
	 * 获取数据库某个表的列表数据
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/list", produces = "application/json; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
    @ResponseBody
    public List<Map<String,Object>> list(HttpServletRequest req) {
		//实例化一个Dbservice对象来操作数据库
    	Dbservice dbm = new Dbservice(databaseService);
    	//获取传输过来的表名然后通过表前缀转换成真实的表名
    	String table = Dbservice.getTableName(req.getParameter("table"));
    	//通过表明实例话一个数据表对象
    	Object tableObj = Dbtablemapping.getModelByTable(table);
    	if (tableObj==null){
    		return null;
		}
    	//用前端传过来的数据来填充数据表对象
    	Object objectObj = Common.getByRequest(tableObj, req, false);
		applyTableQueryAliases(table, objectObj, req);
    	//Robj robj = new Robj();
		//定义一个空的列表用来接收数据库查询的数据
    	List<Map<String,Object>> list = null;
    	try {
    		//准备排序语句
			String ordersql = null;
			String sort = req.getParameter("sort");
			String order = req.getParameter("order");
			//用前端传过来的参数组装排序语句
			if (StringUtil.isNotNullOrEmpty(order)&&StringUtil.isNotNullOrEmpty(sort)){
				ordersql = " order by "+sort+" "+order;
			}
    		//通过数据库实例,和数据表对象来组装的到一个sql语句
			String sql = dbm.list(table,objectObj,ordersql);
			//通过执行sql语句得到数据集
			list = databaseService.find(sql);
			enrichTableRows(table, list);
		} catch (Exception e) {
			
			e.printStackTrace();
		}
    	//把数据集返回到前端
        return list;
    }

	/**
	 * 调用list接口一致只是返回方式不一样
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/listJ", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
    @ResponseBody
    public String listJ(HttpServletRequest req) {
    	List<Map<String,Object>> list = list(req);
        return renderJsonp(list, req);
    }

	/**
	 *查询一个数据库集,操作方式和list接口一直,区别在于这里返回的是单个对象
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/find", produces = "application/json; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
    @ResponseBody
    public Map<String,Object> find(HttpServletRequest req) {
    	Dbservice dbm = new Dbservice(databaseService);
    	String table = Dbservice.getTableName(req.getParameter("table"));
    	Object tableObj = Dbtablemapping.getModelByTable(table);
    	Object objectObj = Common.getByRequest(tableObj, req, false);
		applyTableQueryAliases(table, objectObj, req);
    	//Robj robj = new Robj();
    	List<Map<String,Object>> list = null;
    	try {
			String sql = dbm.list(table,objectObj,true);
			list = databaseService.find(sql);
			enrichTableRows(table, list);
		} catch (Exception e) {
			
			e.printStackTrace();
		}
    	if (list!=null && list.size()>0) {
    		return list.get(0);
		}else{
			return null;
		}
        
    }

	/**
	 * 调用findj通过jsonp返回给客户端
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/findJ", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
    @ResponseBody
    public String findJ(HttpServletRequest req) {
		return renderJsonp(find(req), req);
    }

	/**
	 * 分页查询数据
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/listPage", produces = "application/json; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
    @ResponseBody
    public Page listPage(HttpServletRequest req) {
		//实例化一个Dbservice对象来操作数据库
    	Dbservice dbm = new Dbservice(databaseService);
		//获取传输过来的表名然后通过表前缀转换成真实的表名
    	String table = Dbservice.getTableName(req.getParameter("table"));
    	//实例化一个page对象
    	Page page = new Page();
    	//Robj robj = new Robj();
    	try {
			//通过表明实例话一个数据表对象
    		Object tableObj = Dbtablemapping.getModelByTable(table);
    		Object model = Common.getByRequest(tableObj, req, false);
			applyTableQueryAliases(table, model, req);
    		page.model = model;
    		//获取排序字段
    		String order = req.getParameter("order");
    		//获取排序方式
			String sort = req.getParameter("sort");
			//获取显示第几页
			String pageNo = req.getParameter("page");
			//获取每页显示多少数据
			String pageSize = req.getParameter("rows");
			if (pageSize==null || pageSize.equals("")){
				pageSize = req.getParameter("limit");
			}
			//调用数据库工具返回page对象
			page = dbm.getByPage(page, table,sort,order,pageNo,pageSize);
			enrichTableRows(table, page.rows);
			enrichTableRows(table, page.data);
	    	//robj.setData(page);
		} catch (Exception e) {
			
			e.printStackTrace();
		}  	
        return page;
    }

	/**
	 * 商品管理：分类筛选项与列表「分类」同源，来自 {@code fs_good.type / ctype} 去重，不用 {@code fs_type} 字典。
	 * 可选参数：{@code sid}（商家只看本店）、{@code btype}（1/2 组合商品页）。
	 */
	@RequestMapping(value = "/listGoodCategoryFilters", produces = "application/json; charset=utf-8", method = {
			RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public List<Map<String, Object>> listGoodCategoryFilters(HttpServletRequest req) {
		List<Map<String, Object>> out = new ArrayList<>();
		try {
			String table = Dbservice.getTableName("good");
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT DISTINCT TRIM(IFNULL(type,'')) AS tv, TRIM(IFNULL(ctype,'')) AS cv FROM ").append(table);
			sql.append(" WHERE 1=1 ");
			String sid = req.getParameter("sid");
			if (sid != null && sid.trim().matches("\\d+")) {
				sql.append(" AND sid=").append(sid.trim());
			}
			String btype = req.getParameter("btype");
			if (btype != null && ("1".equals(btype.trim()) || "2".equals(btype.trim()))) {
				sql.append(" AND btype=").append(btype.trim());
			}
			sql.append(" AND (TRIM(IFNULL(type,''))<>'' OR TRIM(IFNULL(ctype,''))<>'') ");
			sql.append(" ORDER BY tv ASC, cv ASC");
			List<Map<String, Object>> rows = databaseService.find(sql.toString());
			if (rows == null) {
				return out;
			}
			LinkedHashSet<String> seen = new LinkedHashSet<>();
			for (Map<String, Object> row : rows) {
				String tv = normalizeGoodCatField(row.get("tv"));
				String cv = normalizeGoodCatField(row.get("cv"));
				if (tv.isEmpty() && cv.isEmpty()) {
					continue;
				}
				String label = goodShelfCategoryLabel(tv, cv);
				if (label.isEmpty()) {
					continue;
				}
				String dedupe = tv + "\0" + cv;
				if (!seen.add(dedupe)) {
					continue;
				}
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("label", label);
				m.put("tv", tv);
				m.put("cv", cv);
				out.add(m);
			}
			out.sort(Comparator.comparing(m -> String.valueOf(m.get("label"))));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return out;
	}

	private static String normalizeGoodCatField(Object o) {
		if (o == null) {
			return "";
		}
		return String.valueOf(o).trim();
	}

	/** 与商品列表「分类」展示一致：优先 {@code type}（货架短名），否则 {@code ctype} */
	private static String goodShelfCategoryLabel(String tv, String cv) {
		if (tv != null && !tv.trim().isEmpty()) {
			return tv.trim();
		}
		if (cv != null && !cv.trim().isEmpty()) {
			return cv.trim();
		}
		return "";
	}

	/**
	 * 通过sql语句返回分页对象
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/listPageSql", produces = "application/json; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
	@ResponseBody
	public Page listPageSql(HttpServletRequest req) {
		//实例化一个Dbservice对象来操作数据库
		Dbservice dbm = new Dbservice(databaseService);
		//获取前端传过来的查询语句
		String sql = req.getParameter("sql");
		//实例化一个page对象
		Page page = new Page();
		//Robj robj = new Robj();
		try {
			page.model = "sql";
			//获取排序字段
			String order = req.getParameter("order");
			//获取排序方式
			String sort = req.getParameter("sort");
			//获取显示第几页
			String pageNo = req.getParameter("page");
			//获取每页显示多少数据
			String pageSize = req.getParameter("rows");
			if (pageSize==null || pageSize.equals("")){
				pageSize = req.getParameter("limit");
			}
			//调用数据库工具返回page对象
			page = dbm.getByPageSql(page, sql,sort,order,pageNo,pageSize);
			if (sql != null) {
				enrichSqlRows(sql, page.rows);
				enrichSqlRows(sql, page.data);
			}
			//robj.setData(page);
		} catch (Exception e) {

			e.printStackTrace();
		}
		return page;
	}

	/**
	 * 通listPage返回jsonp数据
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/listPageJ", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
    @ResponseBody
    public String listPageJ(HttpServletRequest req) {
    	Dbservice dbm = new Dbservice(databaseService);
    	String table = Dbservice.getTableName(req.getParameter("table"));
    	Page page = new Page();
    	//Robj robj = new Robj();
    	try {
    		Object model = Common.getByRequest(Dbtablemapping.getModelByTable(table), req, false);
    		page.model = model;
			page = dbm.getByPage(page, table,null,null,null,null);
	    	//robj.setData(page);
		} catch (Exception e) {
			
			e.printStackTrace();
		}  	
        return renderJsonp(page, req);
    }

	/**
	 * 保存数据的同上传文件
	 * @param files
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/saveWithFile", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
    @ResponseBody
    public String saveWithFile(@RequestParam(value = "file", required = false) MultipartFile[] files,HttpServletRequest req) {
    	Dbservice dbm = new Dbservice(databaseService);
		String rstr = "0";
    	try {
    		//判断文件域里面是否又文件,有文件就上传然后得到文件名
    		String fileNames=null;
    		if(files!=null && files.length>0){
    			long fn = files[0].getSize();
    			if(fn>1){
    				fileNames = Common.copyFile2Upload(files);
    			}
    			
    		}
    		String tableReq = req.getParameter("table");
    		String fileField = req.getParameter("fileName");
			//获取传输过来的表名然后通过表前缀转换成真实的表名
        	String table = Dbservice.getTableName(tableReq);
			//通过表明实例话一个数据表对象
        	Object model = Common.getByRequestSetfile(Dbtablemapping.getModelByTable(table),fileField,fileNames, req, false);
			if (model instanceof User) {
				UserSupport.applySaveRequest((User) model, req);
			}
			SensitiveContentService.CheckResult check = sensitiveContentService.checkBeforeSave(tableReq, model);
			if (!check.pass) {
				return check.message;
			}
			//通过数据库实例,和数据表对象来组装的到一个sql语句
    		String sql = dbm.save(model, table);
    		//执行sql语句操作数据库
    		databaseService.executeAction(sql);
			/**
			 * 判断是否插入操作,如果是返回插入的id
			 */
			String id = req.getParameter("id");

			if(id==null || id.equals("")){
				List<Map<String, Object>> rlist = databaseService.find("select LAST_INSERT_ID() as lastId");
				if(rlist!=null){
					rstr = rlist.get(0).get("lastId")+"";
				}
			}else{
				rstr = id;
			}
			sensitiveContentService.onTableMutated(tableReq);
			syncShopPfAfterReplaySave(table, model);
			syncUserFavoritesAfterSave(table, rstr, req);
		} catch (Exception e) {
			
			return "操作失败"+e.getMessage();
		}
    	
    	return rstr;
        
    }

	/**
	 * 上传文件
	 * @param files
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/upload", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST, RequestMethod.OPTIONS })
    @ResponseBody
    public String upload(@RequestParam(value = "file", required = false) MultipartFile[] files,HttpServletRequest req) {
    	String fileNames=null;
    	try {
    		if(files!=null && files.length>0){
    			fileNames = Common.copyFile2Upload(files);
    		}
    		
		} catch (Exception e) {
		}
    	return fileNames;
        
    }

	/**
	 * 按照编辑器的格式上传视频
	 * @param files
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/editorUploadVideo", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
	@ResponseBody
	public String editorUploadVideo(@RequestParam(value = "file", required = false) MultipartFile[] files,HttpServletRequest req) {
		String fileNames=null;
		String jsonobj = null;
		try {
			if(files!=null && files.length>0){
				fileNames = Common.copyFile2Upload(files);
				ArrayList<String> nl = new ArrayList<String>();
				String[] fns = fileNames.split(",");
				String url = "upload/"+fns[0];
				jsonobj = "{\"errno\":0,\"data\":{\"url\":\""+url+"\"}}";
			}

		} catch (Exception e) {
		}
		return jsonobj;

	}
	/**
	 * 按照编辑器的格式上传视频
	 * @param files
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/editorUploadVideoJ", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
	@ResponseBody
	public String editorUploadVideoJ(@RequestParam(value = "file", required = false) MultipartFile[] files,HttpServletRequest req) {
		String fileNames=null;
		String jsonobj = null;
		try {
			if(files!=null && files.length>0){
				fileNames = Common.copyFile2Upload(files);
				ArrayList<String> nl = new ArrayList<String>();
				String[] fns = fileNames.split(",");
				String url = fns[0];
				jsonobj = "{\"errno\":0,\"data\":{\"url\":\""+url+"\"}}";
			}

		} catch (Exception e) {
		}
		return jsonobj;

	}

	/**
	 * 按照编辑器的格式上传图片
	 * @param files
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/editorUpload", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
	@ResponseBody
	public String editorUpload(@RequestParam(value = "file", required = false) MultipartFile[] files,HttpServletRequest req) {
		String fileNames=null;
		EditorUpload eu = new EditorUpload();
		eu.setErrno(0);
		try {
			if(files!=null && files.length>0){
				fileNames = Common.copyFile2Upload(files);
				ArrayList<String> nl = new ArrayList<String>();
				String[] fns = fileNames.split(",");
				for (int i = 0; i < fns.length; i++) {
					nl.add("upload/"+fns[i]);
				}
				eu.setData(nl);
			}

		} catch (Exception e) {
		}
		return JSON.toJSONString(eu);

	}
	/**
	 * 按照编辑器的格式上传图片
	 * @param files
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/editorUploadJ", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
	@ResponseBody
	public String editorUploadJ(@RequestParam(value = "file", required = false) MultipartFile[] files,HttpServletRequest req) {
		String fileNames=null;
		EditorUpload eu = new EditorUpload();
		eu.setErrno(0);
		try {
			if(files!=null && files.length>0){
				fileNames = Common.copyFile2Upload(files);
				ArrayList<String> nl = new ArrayList<String>();
				String[] fns = fileNames.split(",");
				for (int i = 0; i < fns.length; i++) {
					nl.add(fns[i]);
				}
				eu.setData(nl);
			}

		} catch (Exception e) {
		}
		return JSON.toJSONString(eu);

	}

	/**
	 * 工具方法,可以生成二维码
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/createQrcode", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
    @ResponseBody
    public String createQrcode(HttpServletRequest req) {
    	String fileNames="";
    	try {
    		String path = ResourceUtils.getURL("classpath:").getPath();
            String destPath = path+File.separator+"static"+File.separator+"upload"+File.separator;
            String content = req.getParameter("code");
        	fileNames=QRCodeUtil.encode(content, null, destPath, true);
		} catch (Exception e) {
			
		}
    	return fileNames;
        
    }
	/**
	 * 工具方法,可以生成二维码
	 * @param req
	 * @return
	 */
    @RequestMapping(value = "/createQrcodeJ", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
    @ResponseBody
    public String createQrcodeJ(HttpServletRequest req) {
    	String fileNames="";
    	try {
    		String path = ResourceUtils.getURL("classpath:").getPath();
            String destPath = path+File.separator+"static"+File.separator+"upload"+File.separator;
            String content = req.getParameter("code");
        	fileNames=QRCodeUtil.encode(content, null, destPath, true);
		} catch (Exception e) {
			
		}
    	return renderJsonpString(fileNames, req);
    }

	/**
	 * 返回jsonp格式数据字符串
	 * @param str
	 * @param req
	 * @return
	 */
    private String renderJsonpString(String str,HttpServletRequest req){
    	Map<String,String> obj = new HashMap<>();
		obj.put("info", str);
		String callbackFunctionName = req.getParameter("callback");	
		String json = JSON.toJSONString(obj);
		String r = callbackFunctionName+"("+json+")";
		return r;
    }

	/**
	 * 返回jsonp格式对象
	 * @param obj
	 * @param req
	 * @return
	 */
	private String renderJsonp(Object obj,HttpServletRequest req){
		String callbackFunctionName = req.getParameter("callback");	
		String json = JSON.toJSONString(obj);
		String r = callbackFunctionName+"("+json+")";
		return r;
    }

	/**
	 * 执行更新的sql语句
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/updateSqlJ", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
    @ResponseBody
    public String updateSqlJ(HttpServletRequest req) {
    	String sql = req.getParameter("sql");
    	try {
    		databaseService.executeAction(sql);
		} catch (Exception e) {
			
			return renderJsonpString("0", req);
		}
    	
    	return renderJsonpString("1", req);
        
    }


	/**
	 * 执行查询的sql语句
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/listSqlJ", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
    @ResponseBody
    public String listSqlJ(HttpServletRequest req) {
    	String sql = req.getParameter("sql");
    	List<Map<String,Object>> list = null;
    	try {
			if (sql != null && sql.length() > 0) {
				sql = UriEncoder.decode(sql);
				list = databaseService.find(sql);
				enrichSqlRows(sql, list);
			}
		} catch (Exception e) {
			
			e.printStackTrace();
		}  	
        return renderJsonp(list, req);
    }
    
    
    @RequestMapping(value = "/updateSql", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
    @ResponseBody
    public String updateSql(HttpServletRequest req) {
    	String sql = req.getParameter("sql");
    	try {
    		databaseService.executeAction(sql);
		} catch (Exception e) {
			
			return "操作失败"+e.getMessage();
		}
    	
    	return "操作成功";
        
    }

	@RequestMapping(value = "/listSql", produces = "application/json; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
	@ResponseBody
	public List<Map<String,Object>> listSql(HttpServletRequest req) {
		String sql = req.getParameter("sql");

		List<Map<String,Object>> list = null;
		try {
			if (sql!=null && sql.length()>0){
				sql = UriEncoder.decode(sql);
				list = databaseService.find(sql);
				enrichSqlRows(sql, list);
			}
		} catch (Exception e) {

			e.printStackTrace();
		}
		return list;
	}

	/**
	 * AI 探店文案智能生成（基于文心一言）
	 * 前端传入：imgUrl（主图）、shopName（店铺名，可选）、uid（当前用户，用于取口味 tags）、imgDesc（对图片的简单描述，可选）
	 * 返回：title / content / tags / replies
	 */
	@RequestMapping(value = "/aiGenBlog", produces = "application/json; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> aiGenBlog(HttpServletRequest req) {
		Map<String, Object> result = new HashMap<>();
		try {
			String imgUrl = req.getParameter("imgUrl");
			String shopName = req.getParameter("shopName");
			String imgDesc = req.getParameter("imgDesc");
			String uid = req.getParameter("uid");
			String style = req.getParameter("style");

			// 获取当前用户口味标签 tags
			String tasteTags = "";
			if (uid != null && uid.length() > 0) {
				List<Map<String, Object>> ulist = databaseService.find("select tags from fs_user where id=" + uid);
				if (ulist != null && ulist.size() > 0 && ulist.get(0).get("tags") != null) {
					tasteTags = ulist.get(0).get("tags").toString();
				}
			}

			// 组装提示词 prompt
			StringBuilder prompt = new StringBuilder();
			prompt.append("你是一个本地美食探店文案写作助手。\n\n");
			prompt.append("【店铺名称】：\n").append(shopName == null ? "未提供" : shopName).append("\n\n");
			prompt.append("【用户口味标签】：\n").append(tasteTags == null ? "未提供" : tasteTags).append("\n\n");
			if (imgUrl != null && imgUrl.length() > 0) {
				prompt.append("【图片链接】（仅供你理解场景，不要在文案中出现链接）：\n").append(imgUrl).append("\n\n");
			} else {
				prompt.append("【图片链接】：用户已上传美食照片。\n\n");
			}
			prompt.append("【用户对照片的简单说明】：\n").append(imgDesc == null ? "用户没有额外描述。" : imgDesc).append("\n\n");
			if (style == null || style.length() == 0) {
				style = "简短、生活化、轻松种草风格";
			}
			prompt.append("【文案风格要求】：\n").append(style).append("\n\n");
			prompt.append("请根据以上信息，撰写一段适合发布在小程序上的“探店计划/探店分享”中文文案：\n");
			prompt.append("1. 语气自然、真实，有画面感；\n");
			prompt.append("2. 控制在 60~120 字之内，最多 2 段，每段不超过 2 句；\n");
			prompt.append("3. 重点简单提到菜品口味、店内氛围或适合的场景（如家庭聚餐、朋友小聚等），不要写太细；\n");
			prompt.append("4. 不要生成任何 JSON、键名、英文字段，也不要包含“title”“content”“tags”“replies”等字样；\n");
			prompt.append("5. 不要出现“34号锅底、30号标签、31号套餐”等这类数字编号描述菜品的说法，只用菜名或“辣锅/番茄锅/菌汤锅/招牌菜”等自然中文描述；\n");
			prompt.append("6. 直接输出最终要发布的中文文案内容即可，不要额外解释。\n");

			// 千帆 v2 OpenAI 协议兼容接口调用（鉴权与可选 appid 见 QianfanChatClient）
			String model = Common.getProperty("qianfan_model");
			if (model == null || model.trim().isEmpty()) {
				model = QianfanChatClient.DEFAULT_QIANFAN_MODEL;
			}

			JSONObject body = new JSONObject();
			body.set("model", model.trim());
			JSONArray messages = new JSONArray();
			JSONObject msg = new JSONObject();
			msg.set("role", "user");
			msg.set("content", prompt.toString());
			messages.add(msg);
			body.set("messages", messages);

			String resp = QianfanChatClient.newChatCompletionsRequest()
					.body(body.toString())
					.timeout(120000)
					.execute()
					.body();

			JSONObject respJson = new JSONObject(resp);
			String resultText = "";
			if (respJson.getJSONArray("choices") != null
					&& respJson.getJSONArray("choices").size() > 0) {
				JSONObject choice0 = respJson.getJSONArray("choices").getJSONObject(0);
				if (choice0.getJSONObject("message") != null) {
					resultText = choice0.getJSONObject("message").getStr("content");
				}
			}

			// 直接把大模型返回的文本当作探店计划内容
			if (resultText == null) {
				resultText = "";
			}
			// 可选：给一个默认标题（前端目前主要用 content）
			result.put("title", shopName == null || shopName.length() == 0 ? "本地美食探店计划" : shopName + " 探店计划");
			result.put("content", resultText);
		} catch (Exception e) {
			result.put("title", "本地美食探店");
			result.put("content", "AI 文案生成失败，请稍后重试。错误信息：" + e.getMessage());
		}
		return result;
	}

	/**
	 * 保存数据
	 * @param req
	 * @return
	 */
    @RequestMapping(value = "/save", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
    @ResponseBody
    public String save(HttpServletRequest req) {
    	//实例化一个数据库操作工具实例
    	Dbservice dbm = new Dbservice(databaseService);
    	//Robj robj = new Robj();
    	String tableReq = req.getParameter("table");
		//获取传输过来的表名然后通过表前缀转换成真实的表名
    	String table = Dbservice.getTableName(tableReq);
		//通过表明实例话一个数据表对象
    	Object model = Common.getByRequest(Dbtablemapping.getModelByTable(table), req, false);
		if (model instanceof Bill) {
			Bill bill = (Bill) model;
			BillSupport.applySaveRequest(bill, req);
			if (bill.getState() == null) {
				bill.setState(BillState.PENDING_PAY);
			}
		}
		if (model instanceof Shop) {
			ShopSupport.applySaveRequest((Shop) model, req);
		}
		if (model instanceof Good) {
			GoodSupport.applySaveRequest((Good) model, req);
		}
		if (model instanceof User) {
			UserSupport.applySaveRequest((User) model, req);
		}
		if (model instanceof Youhuiquan) {
			Youhuiquan coupon = (Youhuiquan) model;
			YouhuiquanSupport.applySaveRequest(coupon, req);
			if (coupon.getDeleted() == null) {
				coupon.setDeleted(0);
			}
		}
		if (model instanceof ReplayAppeal) {
			ReplayAppealSupport.applySaveRequest((ReplayAppeal) model, req);
		}
		if (model instanceof ShopQa) {
			ShopQaSupport.applySaveRequest((ShopQa) model, req);
		}
		if (model instanceof Blogplan) {
			BlogplanSupport.applySaveRequest((Blogplan) model, req);
		}
		if (model instanceof Blog) {
			BlogSupport.applySaveRequest((Blog) model, req);
		}
		if (model instanceof Replay) {
			ReplaySupport.applySaveRequest((Replay) model, req);
		}
		applyBlogplanPlantimeChangeReminderReset(table, model);
		SensitiveContentService.CheckResult check = sensitiveContentService.checkBeforeSave(tableReq, model);
		if (!check.pass) {
			return check.message;
		}
    	String rstr = "";
    	try {
			//通过数据库实例,和数据表对象来组装的到一个sql语句
    		String sql = dbm.save(model, table);
    		databaseService.executeAction(sql);
			/**
			 * 判断是否插入操作,如果是返回插入的id
			 */
			String id = req.getParameter("id");

			if(id==null || id.equals("")){
				List<Map<String, Object>> rlist = databaseService.find("select LAST_INSERT_ID() as lastId");
				if(rlist!=null){
					rstr = rlist.get(0).get("lastId")+"";
				}
			}else{
				rstr = id;
			}
			sensitiveContentService.onTableMutated(tableReq);
			syncShopPfAfterReplaySave(table, model);
			syncUserFavoritesAfterSave(table, rstr, req);
		} catch (Exception e) {
			
			return "操作失败"+e.getMessage();
		}
    	
    	return rstr;
        
    }

	/**
	 * 通save方法,返回jsonp格式
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/saveJ", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
    @ResponseBody
    public String saveJ(HttpServletRequest req) {
    	String rstr = save(req);
    	return renderJsonpString(rstr, req);
    }

    
    @RequestMapping(value = "/delete", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
    @ResponseBody
    public String delete(HttpServletRequest req) {
    	Dbservice dbm = new Dbservice(databaseService);
    	//Robj robj = new Robj();
    	String table = Dbservice.getTableName(req.getParameter("table"));
    	Object model = Common.getByRequest(Dbtablemapping.getModelByTable(table), req, false);
		String replayGoodPidForShopPf = null;
		if ("fs_replay".equals(table)) {
			try {
				Integer rid = null;
				if (model instanceof com.ideabobo.model.Replay) {
					rid = ((com.ideabobo.model.Replay) model).getId();
				}
				if (rid == null && req.getParameter("id") != null && req.getParameter("id").length() > 0) {
					rid = Integer.parseInt(req.getParameter("id").trim());
				}
				if (rid != null) {
					List<Map<String, Object>> pre = databaseService.find(
							"SELECT pid, type FROM " + table + " WHERE id = " + rid);
					if (pre != null && !pre.isEmpty()) {
						Map<String, Object> row = pre.get(0);
						Object t = row.get("type");
						if (t instanceof Number && ((Number) t).intValue() == 1) {
							Object p = row.get("pid");
							if (p != null) {
								replayGoodPidForShopPf = p.toString();
							}
						}
					}
				}
			} catch (Exception ignored) {
			}
		}
		try {
    		String sql = dbm.delete(table,model);
    		databaseService.executeAction(sql);
			sensitiveContentService.onTableMutated(table);
    		if (replayGoodPidForShopPf != null) {
    			ShopPfSync.refreshShopPfByGoodId(databaseService, replayGoodPidForShopPf);
    		}
		} catch (Exception e) {
			return "操作失败"+e.getMessage();
		}
    	
    	return "操作成功";
    }

    @RequestMapping(value = "/deleteJ", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
    @ResponseBody
    public String deleteJ(HttpServletRequest req) {
    	delete(req);
    	return renderJsonpString("1", req);
    }

	/**
	 * 获取登录的用户信息
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/getUserInfo", produces = "application/json; charset=utf-8", method = {RequestMethod.GET,
            RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> getUserInfo(HttpServletRequest req) {
        Dbservice dbm = new Dbservice(databaseService);
        String table = Dbservice.getTableName("user");
        Object objectObj = Common.getByRequest(Dbtablemapping.getModelByTable(table), req, false);
        //Robj robj = new Robj();
        List<Map<String, Object>> list = null;
        try {
            String sql = dbm.list(table, objectObj, true);
            list = databaseService.find(sql);
        } catch (Exception e) {

            e.printStackTrace();
        }
        if (list != null && list.size() > 0) {
            Map<String, Object> userinfo = list.get(0);
            UserSupport.enrichRow(databaseService, userinfo);
            Object rt = userinfo.get("roletype");
            userinfo.put("roles", rt == null ? "" : rt.toString());
            return userinfo;
        } else {
            return null;
        }

    }

	/**
	 * 登录方法
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/login", produces = "application/json; charset=utf-8", method = {RequestMethod.GET,
            RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> login(HttpServletRequest req) {
        Dbservice dbm = new Dbservice(databaseService);
        String table = Dbservice.getTableName("user");
        Object objectObj = Common.getByRequest(Dbtablemapping.getModelByTable(table), req, false);
        //Robj robj = new Robj();
        List<Map<String, Object>> list = null;
        try {
            String sql = dbm.list(table, objectObj, true);
            list = databaseService.find(sql);
        } catch (Exception e) {

            e.printStackTrace();
        }
        if (list != null && list.size() > 0) {
            Map<String, Object> userinfo = list.get(0);
            UserSupport.enrichRow(databaseService, userinfo);
            userinfo.put("token", userinfo.get("id").toString());
            Object rt = userinfo.get("roletype");
            userinfo.put("roles", rt == null ? "" : rt.toString());
            return userinfo;
        } else {
            return null;
        }

    }

	/**
	 * 注销登录方法
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/logout", produces = "application/json; charset=utf-8", method = {RequestMethod.GET,
            RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> logout(HttpServletRequest req) {
        Map<String, Object> r = new HashMap<>();
        r.put("code", 200);
        return r;
    }

	/**
	 * 小程序调用接口获取电话号码
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/getWxPhoneNumber", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
	@ResponseBody
	public String getWxPhoneNumber(HttpServletRequest req) {
		// 1.请求微信接口服务，获取accessToken
		JSONObject accessTokenJson = WxLoginUtil.getAccessToken(Common.getProperty("app_id"), Common.getProperty("app_secret"));
		String accessToken = accessTokenJson.get("access_token",String.class);
		// 2.请求微信接口服务，获取用户手机号信息
		String code = req.getParameter("code");
		JSONObject phoneNumberJson = WxLoginUtil.getPhoneNumber(code, accessToken);
		String json = JSON.toJSONString(phoneNumberJson);
		return json;
	}

	/**
	 * 小程序授权登录的方法
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/wxlogin", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
	@ResponseBody
	public String wxlogin(HttpServletRequest req) {
		String appid = Common.getProperty("app_id");
		String app_secret = Common.getProperty("app_secret");
		// 2.请求微信接口服务，获取用户手机号信息
		String code = req.getParameter("code");
		String baseUrl = "https://api.weixin.qq.com/sns/jscode2session?appid="+appid+"&secret="+app_secret+"&js_code="+code+"&grant_type=authorization_code";
		String body = HttpUtil.createGet(baseUrl).execute().body();
		return body;
	}

	/**
	 * 发送短信
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/sendSms", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
	@ResponseBody
	public String sendSms(HttpServletRequest req) {
		String content = req.getParameter("content");
		String phone = req.getParameter("phone");
		String baseUrl = "https://api.smsbao.com/sms?u=ideabobo&p=1FE1982DB0C2045456F501829977926D&m="+phone+"&c="+UriEncoder.encode(content);
		String body = HttpUtil.createGet(baseUrl).execute().body();
		return body;
	}

	/**
	 * 生成图形验证码（小程序端用 captchaId + Base64 图片）
	 */
	@RequestMapping(value = "/captcha", produces = "application/json; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
	@ResponseBody
	public Map<String, String> captcha() {
		LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
		String code = lineCaptcha.getCode();
		String captchaId = CaptchaHolder.store(code);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		lineCaptcha.write(baos);
		String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
		Map<String, String> r = new HashMap<>();
		r.put("captchaId", captchaId);
		r.put("imageBase64", b64);
		return r;
	}

	/**
	 * 账号密码登录（校验图形验证码），返回 errcode：0 成功，1 验证码，2 账号密码
	 */
	@RequestMapping(value = "/userLogin", produces = "application/json; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> userLogin(HttpServletRequest req) {
		Map<String, Object> out = new HashMap<>();
		String captchaId = req.getParameter("captchaId");
		String captchaCode = req.getParameter("captchaCode");
		if (!CaptchaHolder.verifyAndConsume(captchaId, captchaCode)) {
			out.put("errcode", 1);
			out.put("errmsg", "验证码错误或已过期，请重试");
			return out;
		}
		Dbservice dbm = new Dbservice(databaseService);
		String table = Dbservice.getTableName("user");
		Object tableObj = Dbtablemapping.getModelByTable(table);
		if (tableObj == null) {
			out.put("errcode", 2);
			out.put("errmsg", "系统错误");
			return out;
		}
		Object objectObj = Common.getByRequest(tableObj, req, false);
		if (objectObj instanceof User) {
			UserSupport.applyListRequest((User) objectObj, req);
		}
		List<Map<String, Object>> list = null;
		try {
			String sql = dbm.list(table, objectObj, true);
			list = databaseService.find(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (list != null && list.size() > 0) {
			UserSupport.enrichRow(databaseService, list.get(0));
			out.put("errcode", 0);
			out.put("user", list.get(0));
			return out;
		}
		out.put("errcode", 2);
		out.put("errmsg", "账号或密码错误");
		return out;
	}

	/**
	 * 探店计划改期：plantime 与库中旧值不一致时强制 remind_3d_sent=0。
	 * 通用 {@link Dbservice#update} 会跳过 null 整型字段，仅靠前端传 remind_3d_sent 不可靠。
	 */
	private void applyBlogplanPlantimeChangeReminderReset(String fullTable, Object model) {
		if (!"fs_blogplan".equals(fullTable) || !(model instanceof Blogplan)) {
			return;
		}
		Blogplan bp = (Blogplan) model;
		if (bp.getId() == null) {
			return;
		}
		Map<String, Object> row = databaseService.findFirst("SELECT plantime FROM " + fullTable + " WHERE id=" + bp.getId());
		if (row == null || row.isEmpty()) {
			return;
		}
		Object old = row.get("plantime");
		if (old == null) {
			old = row.get("PLANTIME");
		}
		String oldPt = old == null ? "" : String.valueOf(old).trim();
		String newPt = bp.getPlantime() == null ? "" : bp.getPlantime().trim();
		if (!newPt.equals(oldPt)) {
			bp.setRemind_3d_sent(0);
		}
	}

	private void syncUserFavoritesAfterSave(String table, String savedId, HttpServletRequest req) {
		if (!UserSupport.isUserTable(table) || savedId == null || savedId.trim().isEmpty() || req == null) {
			return;
		}
		try {
			UserSupport.syncFavoritesAfterSave(databaseService, Integer.parseInt(savedId.trim()), req);
		} catch (NumberFormatException ignored) {
		}
	}

	/** 菜品评价（replay.type=1）保存后刷新对应店铺汇总评分 */
	private void syncShopPfAfterReplaySave(String table, Object model) {
		if (!"fs_replay".equals(table) || !(model instanceof com.ideabobo.model.Replay)) {
			return;
		}
		com.ideabobo.model.Replay replayRow = (com.ideabobo.model.Replay) model;
		if (replayRow.getType() != null && replayRow.getType() == 1 && replayRow.getPid() != null) {
			ShopPfSync.refreshShopPfByGoodId(databaseService, String.valueOf(replayRow.getPid()));
			Integer gid = replayRow.getPid();
			if (gid != null) {
				Integer sid = null;
				try {
					List<Map<String, Object>> rs = databaseService.find("select sid from fs_good where id=" + gid + " limit 1");
					if (rs != null && !rs.isEmpty() && rs.get(0).get("sid") != null) {
						sid = parseSidOrNull(rs.get(0).get("sid").toString());
					}
				} catch (Exception ignored) {
				}
				if (sid != null) {
					refreshShopWordCloudCache(sid, 7, 60, 2);
					refreshShopWordCloudCache(sid, 30, 60, 2);
					refreshShopWordCloudCache(sid, 90, 60, 2);
				}
			}
		}
		if (replayRow.getType() != null && replayRow.getType() == 9 && replayRow.getPid() != null) {
			Integer sid = replayRow.getPid();
			if (sid != null) {
				refreshShopWordCloudCache(sid, 7, 60, 2);
				refreshShopWordCloudCache(sid, 30, 60, 2);
				refreshShopWordCloudCache(sid, 90, 60, 2);
			}
		}
	}

	private static final Pattern DIGITS_ID = Pattern.compile("^\\d+$");

	private static Set<String> splitCsvIds(String csv) {
		Set<String> s = new HashSet<>();
		if (csv == null) {
			return s;
		}
		String t = csv.trim();
		if (t.isEmpty()) {
			return s;
		}
		for (String p : t.split(",")) {
			String x = p.trim();
			if (!x.isEmpty()) {
				s.add(x);
			}
		}
		return s;
	}

	/** 保持用户在档案里填写标签的顺序，便于展示与 AI 说明 */
	private static List<String> orderedCsvIds(String csv) {
		List<String> out = new ArrayList<>();
		if (csv == null) {
			return out;
		}
		String t = csv.trim();
		if (t.isEmpty()) {
			return out;
		}
		for (String p : t.split(",")) {
			String x = p.trim();
			if (!x.isEmpty()) {
				out.add(x);
			}
		}
		return out;
	}

	private static double roundRec(double x) {
		return Math.round(x * 100.0) / 100.0;
	}

	/** 将品类 id 解析为 fs_type.title，供前端展示「是否命中口味标签」 */
	private List<String> resolveTagTitles(List<String> orderedIds) {
		List<String> titles = new ArrayList<>();
		if (orderedIds == null || orderedIds.isEmpty()) {
			return titles;
		}
		List<String> numericIds = new ArrayList<>();
		for (String id : orderedIds) {
			if (id == null) {
				continue;
			}
			String tr = id.trim();
			if (DIGITS_ID.matcher(tr).matches()) {
				numericIds.add(tr);
			}
		}
		if (numericIds.isEmpty()) {
			return titles;
		}
		String inList = String.join(",", numericIds);
		Map<String, String> idToTitle = new HashMap<>();
		try {
			List<Map<String, Object>> types = databaseService.find("select id,title from fs_type where id in (" + inList + ")");
			if (types != null) {
				for (Map<String, Object> row : types) {
					if (row.get("id") == null) {
						continue;
					}
					String tid = row.get("id").toString().trim();
					String tit = row.get("title") == null ? "" : row.get("title").toString().trim();
					idToTitle.put(tid, tit.isEmpty() ? ("#" + tid) : tit);
				}
			}
		} catch (Exception ignored) {
		}
		for (String id : orderedIds) {
			if (id == null) {
				continue;
			}
			String tr = id.trim();
			if (!DIGITS_ID.matcher(tr).matches()) {
				continue;
			}
			String tit = idToTitle.get(tr);
			titles.add(tit != null ? tit : ("#" + tr));
		}
		return titles;
	}

	private static String buildFactorNarrative(boolean fCollab, boolean fOrder, boolean fTag, boolean fPf, boolean fBlog,
			boolean fPlan, boolean fSales, boolean fGlobal, boolean fFav, boolean fNearby, boolean hasUserLoc) {
		List<String> z = new ArrayList<>();
		if (fNearby) {
			z.add(hasUserLoc ? "用户定位与距离衰减" : "距离因子已开但未传经纬度");
		}
		if (fTag) {
			z.add("用户口味标签与店铺品类匹配");
		}
		if (fPf) {
			z.add("店铺评分");
		}
		if (fFav) {
			z.add("用户收藏");
		}
		if (fCollab) {
			z.add("相似用户收藏协同过滤");
		}
		if (fOrder) {
			z.add("用户历史订单复购");
		}
		if (fPlan) {
			z.add("已完成探店计划");
		}
		if (fBlog) {
			z.add("探店动态互动热度");
		}
		if (fSales) {
			z.add("门店商品销量");
		}
		if (fGlobal) {
			z.add("全站订单热度");
		}
		if (z.isEmpty()) {
			return "当前未启用任何数值打分因子。";
		}
		return "本次多因子排序已纳入：" + String.join("；", z) + "。";
	}

	private static double jaccard(Set<String> a, Set<String> b) {
		if (a.isEmpty() && b.isEmpty()) {
			return 0;
		}
		int inter = 0;
		for (String x : a) {
			if (b.contains(x)) {
				inter++;
			}
		}
		int uni = a.size() + b.size() - inter;
		return uni <= 0 ? 0 : (double) inter / (double) uni;
	}

	private static double parsePf(Object o) {
		if (o == null) {
			return 0;
		}
		try {
			String s = o.toString().trim();
			if (s.isEmpty()) {
				return 0;
			}
			return Double.parseDouble(s);
		} catch (Exception e) {
			return 0;
		}
	}

	private static long parseLongObj(Object o, long def) {
		if (o == null) {
			return def;
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (Exception e) {
			return def;
		}
	}

	/** 推荐因子开关：参数缺省为开启(1)，传 0 / false 关闭 */
	private static boolean recFactorOn(HttpServletRequest req, String key, boolean defaultOn) {
		String v = req.getParameter(key);
		if (v == null || v.trim().isEmpty()) {
			return defaultOn;
		}
		String t = v.trim();
		return !("0".equals(t) || "false".equalsIgnoreCase(t) || "off".equalsIgnoreCase(t));
	}

	private static double parseDoubleParam(String s, double def) {
		if (s == null || s.trim().isEmpty()) {
			return def;
		}
		try {
			return Double.parseDouble(s.trim());
		} catch (Exception e) {
			return def;
		}
	}

	/** 地球表面两点距离（千米） */
	private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
		final double R = 6371.0;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return R * c;
	}

	private static boolean readShopLatLng(Object latObj, Object lngObj, double[] outLatLon) {
		if (latObj == null || lngObj == null) {
			return false;
		}
		try {
			double la = Double.parseDouble(latObj.toString().trim());
			double ln = Double.parseDouble(lngObj.toString().trim());
			if (Math.abs(la) < 1e-6 && Math.abs(ln) < 1e-6) {
				return false;
			}
			outLatLon[0] = la;
			outLatLon[1] = ln;
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 按直线距离（千米）取距用户最近的若干店铺，供 AI 小助手「附近推荐」类问题使用；坐标系需与客户端 getLocation 一致（小程序一般为 gcj02）。
	 */
	private List<Map<String, Object>> buildNearbyShopsPreview(double userLat, double userLng, int limit) {
		if (limit < 1) {
			limit = 1;
		}
		if (limit > 20) {
			limit = 20;
		}
		List<Map<String, Object>> shops = databaseService.find(
				"select id,sname,pf,latitude,longitude from fs_shop where state=" + ShopAuditState.APPROVED + " order by id desc");
		if (shops == null || shops.isEmpty()) {
			shops = databaseService.find(
					"select id,sname,pf,latitude,longitude from fs_shop order by id desc limit 200");
		}
		if (shops == null) {
			shops = Collections.emptyList();
		}
		double[] shopLL = new double[2];
		List<Map<String, Object>> withDist = new ArrayList<>();
		for (Map<String, Object> sh : shops) {
			if (sh.get("id") == null) {
				continue;
			}
			if (!readShopLatLng(sh.get("latitude"), sh.get("longitude"), shopLL)) {
				continue;
			}
			double km = haversineKm(userLat, userLng, shopLL[0], shopLL[1]);
			Map<String, Object> one = new LinkedHashMap<>();
			one.put("id", sh.get("id"));
			one.put("sname", sh.get("sname") == null ? "" : sh.get("sname").toString());
			one.put("pf", sh.get("pf"));
			one.put("distanceKm", Math.round(km * 10.0) / 10.0);
			withDist.add(one);
		}
		Collections.sort(withDist, new Comparator<Map<String, Object>>() {
			@Override
			public int compare(Map<String, Object> a, Map<String, Object> b) {
				double da = ((Number) a.get("distanceKm")).doubleValue();
				double db = ((Number) b.get("distanceKm")).doubleValue();
				return Double.compare(da, db);
			}
		});
		if (withDist.size() <= limit) {
			return withDist;
		}
		return new ArrayList<>(withDist.subList(0, limit));
	}

	/**
	 * 多因子店铺推荐：协同（收藏 Jaccard + 相似用户加权）、个人订单、口味标签、店铺评分、探店动态热度、已完成探店计划、店内商品销量；
	 * 可选 useAi=1 时用千帆大模型对 Top 候选重排并生成一句推荐导语（失败则回退数值排序）。
	 *
	 * 参数：uid（可选），limit（默认15），useAi（0/1）；
	 * 因子开关（缺省均为 1=启用，0=关闭）：collab, order, tag, pf, blog, plan, sales, global, fav, nearby, div, ai；
	 * 附近：lat、lng（用户当前坐标，WGS84），nearKm（距离衰减尺度，默认 12km，越大越远仍有一定分）。
	 */
	@RequestMapping(value = "/recommendShops", produces = "application/json; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> recommendShops(HttpServletRequest req) {
		Map<String, Object> out = new HashMap<>();
		List<Map<String, Object>> empty = new ArrayList<>();
		out.put("list", empty);
		out.put("aiHint", "");
		out.put("prefsEcho", new HashMap<String, Object>());
		out.put("tagLabels", new ArrayList<String>());
		out.put("metricPreview", null);
		out.put("aiKeyConfigured", false);
		out.put("aiInvoked", false);
		out.put("aiRerankApplied", false);
		try {
			String uidStr = req.getParameter("uid");
			int limit = 15;
			if (req.getParameter("limit") != null) {
				try {
					limit = Math.min(50, Math.max(1, Integer.parseInt(req.getParameter("limit").trim())));
				} catch (Exception ignored) {
				}
			}
			boolean facAi = recFactorOn(req, "ai", true);
			boolean useAi = facAi && ("1".equals(req.getParameter("useAi")) || "true".equalsIgnoreCase(req.getParameter("useAi")));
			String qfKeyProp = Common.getProperty("qianfan_api_key");
			boolean aiKeyConfigured = qfKeyProp != null && qfKeyProp.trim().length() > 0;
			out.put("aiKeyConfigured", aiKeyConfigured);
			out.put("aiInvoked", useAi);

			boolean fCollab = recFactorOn(req, "collab", true);
			boolean fOrder = recFactorOn(req, "order", true);
			boolean fTag = recFactorOn(req, "tag", true);
			boolean fPf = recFactorOn(req, "pf", true);
			boolean fBlog = recFactorOn(req, "blog", true);
			boolean fPlan = recFactorOn(req, "plan", true);
			boolean fSales = recFactorOn(req, "sales", true);
			boolean fGlobal = recFactorOn(req, "global", true);
			boolean fFav = recFactorOn(req, "fav", true);
			boolean fNearby = recFactorOn(req, "nearby", true);
			boolean fDiv = recFactorOn(req, "div", true);

			double userLat = parseDoubleParam(req.getParameter("lat"), Double.NaN);
			double userLng = parseDoubleParam(req.getParameter("lng"), Double.NaN);
			boolean hasUserLoc = !Double.isNaN(userLat) && !Double.isNaN(userLng);
			double nearKm = parseDoubleParam(req.getParameter("nearKm"), 12.0);
			if (nearKm < 1) {
				nearKm = 1;
			}
			if (nearKm > 200) {
				nearKm = 200;
			}

			Map<String, Object> echo = new HashMap<>();
			echo.put("collab", fCollab);
			echo.put("order", fOrder);
			echo.put("tag", fTag);
			echo.put("pf", fPf);
			echo.put("blog", fBlog);
			echo.put("plan", fPlan);
			echo.put("sales", fSales);
			echo.put("global", fGlobal);
			echo.put("fav", fFav);
			echo.put("nearby", fNearby);
			echo.put("div", fDiv);
			echo.put("ai", facAi);
			echo.put("hasUserLoc", hasUserLoc);
			echo.put("nearKm", nearKm);
			out.put("prefsEcho", echo);

			Set<String> myFavs = new HashSet<>();
			Set<String> myTags = new HashSet<>();
			List<String> myTagOrder = new ArrayList<>();
			boolean hasUser = uidStr != null && DIGITS_ID.matcher(uidStr.trim()).matches();
			if (hasUser) {
				int uidInt = Integer.parseInt(uidStr.trim());
				myFavs = UserSupport.loadFavoriteIdSet(databaseService, uidInt, UserSupport.FAV_SHOP);
				List<Map<String, Object>> meList = databaseService
						.find("select tags from fs_user where id=" + uidInt + " limit 1");
				if (meList != null && meList.size() > 0) {
					Map<String, Object> me = meList.get(0);
					String tagsRaw = me.get("tags") == null ? "" : me.get("tags").toString();
					myTags = splitCsvIds(tagsRaw);
					myTagOrder = orderedCsvIds(tagsRaw);
				}
			}
			List<String> tagLabels = resolveTagTitles(myTagOrder);
			out.put("tagLabels", tagLabels);

			String factorNarrative = buildFactorNarrative(fCollab, fOrder, fTag, fPf, fBlog, fPlan, fSales, fGlobal, fFav,
					fNearby, hasUserLoc);

			List<Map<String, Object>> shops = databaseService.find(
					"select * from fs_shop where state=" + ShopAuditState.APPROVED + " order by id desc");
			if (shops == null || shops.isEmpty()) {
				shops = databaseService.find("select * from fs_shop order by id desc limit 100");
			}
			if (shops == null) {
				shops = new ArrayList<>();
			}

			/* 全站订单量（店铺热度） */
			Map<String, Long> globalOrderCnt = new HashMap<>();
			if (fGlobal) {
				try {
					List<Map<String, Object>> goc = databaseService.find(
							"select sid, count(*) as c from fs_bill where sid is not null and sid<>'' group by sid");
					if (goc != null) {
						for (Map<String, Object> r : goc) {
							if (r.get("sid") == null) {
								continue;
							}
							globalOrderCnt.put(r.get("sid").toString().trim(), parseLongObj(r.get("c"), 0));
						}
					}
				} catch (Exception ignored) {
				}
			}

			/* 当前用户订单店铺次数 */
			Map<String, Long> myOrderCnt = new HashMap<>();
			if (fOrder && hasUser) {
				try {
					List<Map<String, Object>> moc = databaseService.find("select sid, count(*) as c from fs_bill where uid='"
							+ uidStr.trim() + "' and sid is not null and sid<>'' group by sid");
					if (moc != null) {
						for (Map<String, Object> r : moc) {
							if (r.get("sid") == null) {
								continue;
							}
							myOrderCnt.put(r.get("sid").toString().trim(), parseLongObj(r.get("c"), 0));
						}
					}
				} catch (Exception ignored) {
				}
			}

			/* 动态热度：赞+浏览+收藏 */
			Map<String, Long> blogHeat = new HashMap<>();
			if (fBlog) {
				try {
					List<Map<String, Object>> bh = databaseService.find(
							"select sid, sum(coalesce(zan,0)+coalesce(vcount,0)+coalesce(favcount,0)) as h from fs_blog where sid is not null group by sid");
					if (bh != null) {
						for (Map<String, Object> r : bh) {
							if (r.get("sid") == null) {
								continue;
							}
							blogHeat.put(r.get("sid").toString().trim(), parseLongObj(r.get("h"), 0));
						}
					}
				} catch (Exception ignored) {
				}
			}

			/* 用户已完成探店计划店铺 */
			Set<String> planSids = new HashSet<>();
			if (fPlan && hasUser) {
				try {
					List<Map<String, Object>> pl = databaseService.find(
							"select sid from fs_blogplan where uid=" + uidStr.trim() + " and state="
									+ BlogplanState.COMPLETED + " and sid is not null");
					if (pl != null) {
						for (Map<String, Object> r : pl) {
							if (r.get("sid") != null) {
								planSids.add(r.get("sid").toString().trim());
							}
						}
					}
				} catch (Exception ignored) {
				}
			}

			/* 店铺商品销量合计 */
			Map<String, Long> goodXlSum = new HashMap<>();
			if (fSales) {
				try {
					List<Map<String, Object>> gx = databaseService
							.find("select sid, sum(coalesce(xl,0)) as sx from fs_good where sid is not null group by sid");
					if (gx != null) {
						for (Map<String, Object> r : gx) {
							if (r.get("sid") == null) {
								continue;
							}
							goodXlSum.put(r.get("sid").toString().trim(), parseLongObj(r.get("sx"), 0));
						}
					}
				} catch (Exception ignored) {
				}
			}

			/* 协同：与当前用户收藏 Jaccard 最高的若干用户，按其相似度给「他人收藏店铺」加权 */
			Map<String, Double> collabShopW = new HashMap<>();
			if (fCollab && hasUser && !myFavs.isEmpty()) {
				try {
					int uidInt = Integer.parseInt(uidStr.trim());
					Map<Integer, Set<String>> allShopFav = UserSupport.loadAllUserShopFavoriteMap(databaseService,
							uidInt);
					List<double[]> simRows = new ArrayList<>();
					for (Map.Entry<Integer, Set<String>> e : allShopFav.entrySet()) {
						Set<String> uf = e.getValue();
						if (uf == null || uf.isEmpty()) {
							continue;
						}
						double sim = jaccard(myFavs, uf);
						if (sim > 0) {
							simRows.add(new double[] { e.getKey().doubleValue(), sim });
						}
					}
					Collections.sort(simRows, new Comparator<double[]>() {
						@Override
						public int compare(double[] a, double[] b) {
							return Double.compare(b[1], a[1]);
						}
					});
					int topK = Math.min(12, simRows.size());
					for (int i = 0; i < topK; i++) {
						double sim = simRows.get(i)[1];
						int oid = (int) simRows.get(i)[0];
						Set<String> theirs = allShopFav.get(oid);
						if (theirs == null) {
							continue;
						}
						for (String sid : theirs) {
							if (!myFavs.contains(sid)) {
								collabShopW.put(sid, collabShopW.getOrDefault(sid, 0.0) + sim);
							}
						}
					}
				} catch (Exception ignored) {
				}
			}

			long maxBlogH = 1;
			for (long v : blogHeat.values()) {
				if (v > maxBlogH) {
					maxBlogH = v;
				}
			}
			long maxGoc = 1;
			for (long v : globalOrderCnt.values()) {
				if (v > maxGoc) {
					maxGoc = v;
				}
			}

			final double W_COLLAB = 42.0;
			final double W_MY_ORDER = 28.0;
			final double W_TAG = 32.0;
			final double W_PF = 9.0;
			final double W_BLOG = 7.0;
			final double W_PLAN = 14.0;
			final double W_GOOD_XL = 4.0;
			final double W_GLOBAL_ORD = 11.0;
			final double W_FAV = 6.0;
			final double W_NEAR = 26.0;

			double[] shopLL = new double[2];
			List<Map<String, Object>> scored = new ArrayList<>();
			for (Map<String, Object> sh : shops) {
				if (sh.get("id") == null) {
					continue;
				}
				String sid = sh.get("id").toString().trim();
				double score = 0;
				double cPart = fCollab ? W_COLLAB * collabShopW.getOrDefault(sid, 0.0) : 0;
				double oPart = fOrder ? W_MY_ORDER * Math.log1p(myOrderCnt.getOrDefault(sid, 0L)) : 0;
				String tid = sh.get("typeid") == null ? "" : sh.get("typeid").toString().trim();
				double tagPart = (fTag && myTags.contains(tid) && !tid.isEmpty()) ? W_TAG : 0;
				double pfPart = fPf ? W_PF * parsePf(sh.get("pf")) : 0;
				double bh = blogHeat.getOrDefault(sid, 0L);
				double blogPart = fBlog ? W_BLOG * ((double) bh / (double) maxBlogH) : 0;
				double planPart = (fPlan && planSids.contains(sid)) ? W_PLAN : 0;
				double gxl = goodXlSum.getOrDefault(sid, 0L);
				double gPart = fSales ? W_GOOD_XL * Math.log1p(gxl) : 0;
				double go = globalOrderCnt.getOrDefault(sid, 0L);
				double goPart = fGlobal ? W_GLOBAL_ORD * ((double) go / (double) maxGoc) : 0;
				double favPart = (fFav && myFavs.contains(sid)) ? W_FAV : 0;
				double nearPart = 0;
				if (fNearby && hasUserLoc && readShopLatLng(sh.get("latitude"), sh.get("longitude"), shopLL)) {
					double dkm = haversineKm(userLat, userLng, shopLL[0], shopLL[1]);
					nearPart = W_NEAR * Math.exp(-dkm / nearKm);
				}
				score = cPart + oPart + tagPart + pfPart + blogPart + planPart + gPart + goPart + favPart + nearPart;

				Map<String, Object> row = new HashMap<>(sh);
				double scoreR = roundRec(score);
				row.put("_rec_score", scoreR);
				row.put("_rec_parts", String.format(
						"协同:%.2f 订单:%.2f 标签:%.2f 评分:%.2f 动态:%.2f 计划:%.2f 销量:%.2f 热度:%.2f 收藏:%.2f 附近:%.2f", cPart,
						oPart, tagPart, pfPart, blogPart, planPart, gPart, goPart, favPart, nearPart));
				Map<String, Object> recMetrics = new LinkedHashMap<>();
				recMetrics.put("综合", scoreR);
				recMetrics.put("协同", roundRec(cPart));
				recMetrics.put("订单", roundRec(oPart));
				recMetrics.put("标签", roundRec(tagPart));
				recMetrics.put("评分", roundRec(pfPart));
				recMetrics.put("动态", roundRec(blogPart));
				recMetrics.put("计划", roundRec(planPart));
				recMetrics.put("销量", roundRec(gPart));
				recMetrics.put("热度", roundRec(goPart));
				recMetrics.put("收藏", roundRec(favPart));
				recMetrics.put("附近", roundRec(nearPart));
				row.put("recMetrics", recMetrics);
				scored.add(row);
			}

			Collections.sort(scored, new Comparator<Map<String, Object>>() {
				@Override
				public int compare(Map<String, Object> a, Map<String, Object> b) {
					double sa = ((Number) a.get("_rec_score")).doubleValue();
					double sb = ((Number) b.get("_rec_score")).doubleValue();
					return Double.compare(sb, sa);
				}
			});

			/* 多样性：同 typeid 连续出现时降低保留优先级（轻量 MMR） */
			int cap0 = Math.min(limit, Math.max(0, scored.size()));
			List<Map<String, Object>> diversified = fDiv ? diversifyByTypeid(scored, limit)
					: new ArrayList<>(scored.subList(0, cap0));

			String aiHint = "";
			boolean aiRerankApplied = false;
			if (useAi && !diversified.isEmpty()) {
				try {
					String reranked = aiRerankShopIds(uidStr, myTags, myFavs, tagLabels, factorNarrative, diversified, limit);
					if (reranked != null && !reranked.isEmpty()) {
						diversified = reorderByIdList(diversified, reranked, limit);
						aiRerankApplied = true;
					}
					aiHint = aiRecommendHint(uidStr, myTags, myFavs, tagLabels, factorNarrative, diversified);
				} catch (Exception ex) {
					aiHint = "";
				}
			}
			out.put("aiRerankApplied", aiRerankApplied);

			if (diversified.size() > limit) {
				diversified = new ArrayList<>(diversified.subList(0, limit));
			}
			out.put("list", diversified);
			out.put("aiHint", aiHint == null ? "" : aiHint);
			if (!diversified.isEmpty() && diversified.get(0).get("recMetrics") != null) {
				out.put("metricPreview", diversified.get(0).get("recMetrics"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			out.put("list", empty);
			out.put("aiHint", "");
		}
		return out;
	}

	/** 同品类连续出现时间隔：简单提升列表多元性 */
	private List<Map<String, Object>> diversifyByTypeid(List<Map<String, Object>> sorted, int limit) {
		List<Map<String, Object>> pool = new ArrayList<>(sorted);
		List<Map<String, Object>> out = new ArrayList<>();
		String lastType = null;
		int streak = 0;
		while (!pool.isEmpty() && out.size() < limit) {
			Map<String, Object> pick = null;
			int pickIdx = -1;
			if (streak >= 2 && lastType != null) {
				for (int i = 0; i < pool.size(); i++) {
					String t = pool.get(i).get("typeid") == null ? "" : pool.get(i).get("typeid").toString();
					if (!t.equals(lastType)) {
						pick = pool.get(i);
						pickIdx = i;
						break;
					}
				}
			}
			if (pick == null) {
				pick = pool.get(0);
				pickIdx = 0;
			}
			pool.remove(pickIdx);
			out.add(pick);
			String t = pick.get("typeid") == null ? "" : pick.get("typeid").toString();
			if (t.equals(lastType)) {
				streak++;
			} else {
				lastType = t;
				streak = 1;
			}
		}
		return out;
	}

	private List<Map<String, Object>> reorderByIdList(List<Map<String, Object>> rows, String idOrderCsv, int limit) {
		String[] parts = idOrderCsv.split(",");
		List<Map<String, Object>> byId = new ArrayList<>();
		Map<String, Map<String, Object>> map = new HashMap<>();
		for (Map<String, Object> r : rows) {
			if (r.get("id") != null) {
				map.put(r.get("id").toString().trim(), r);
			}
		}
		Set<String> seen = new HashSet<>();
		for (String p : parts) {
			String id = p.trim();
			if (id.isEmpty() || seen.contains(id)) {
				continue;
			}
			Map<String, Object> m = map.get(id);
			if (m != null) {
				byId.add(m);
				seen.add(id);
			}
			if (byId.size() >= limit) {
				break;
			}
		}
		for (Map<String, Object> r : rows) {
			if (byId.size() >= limit) {
				break;
			}
			String id = r.get("id").toString().trim();
			if (!seen.contains(id)) {
				byId.add(r);
				seen.add(id);
			}
		}
		return byId;
	}

	private String aiRerankShopIds(String uid, Set<String> myTags, Set<String> myFavs, List<String> tagLabelList,
			String factorNarrative, List<Map<String, Object>> candidates, int limit) {
		String apiKey = Common.getProperty("qianfan_api_key");
		if (apiKey == null || apiKey.trim().isEmpty()) {
			return null;
		}
		String model = Common.getProperty("qianfan_model");
		if (model == null || model.trim().isEmpty()) {
			model = QianfanChatClient.DEFAULT_QIANFAN_MODEL;
		}
		String tagNamesHuman = (tagLabelList == null || tagLabelList.isEmpty()) ? "未设置" : String.join("、", tagLabelList);
		String tagStr = String.join(",", myTags);
		StringBuilder cand = new StringBuilder();
		int n = Math.min(24, candidates.size());
		for (int i = 0; i < n; i++) {
			Map<String, Object> s = candidates.get(i);
			String id = s.get("id").toString();
			String name = s.get("sname") == null ? "" : s.get("sname").toString();
			String tc = s.get("typecn") == null ? "" : s.get("typecn").toString();
			Object sc = s.get("_rec_score");
			String metricBrief = formatRecMetricsBrief(s.get("recMetrics"));
			cand.append(id).append("|").append(name).append("|").append(tc).append("|").append(sc).append("|")
					.append(metricBrief).append("; ");
		}
		String prompt = "你是本地美食小程序的智能推荐排序助手。请结合「算法已算出的多因子得分」与「用户画像」对候选店重新排序。\n"
				+ "用户ID:" + (uid == null ? "无" : uid) + "\n" + "用户口味标签（中文名）:" + tagNamesHuman + "\n" + "对应品类id:" + tagStr + "\n"
				+ "用户已收藏店铺id:" + String.join(",", myFavs) + "\n" + factorNarrative + "\n"
				+ "候选店铺(格式 id|店名|品类|算法综合分|非零分项摘要): " + cand.toString() + "\n"
				+ "任务：输出你认为最合适的推荐顺序的店铺id，仅一行英文逗号分隔数字，不要其它文字。要求：尊重口味标签与多因子得分，兼顾品类多元、适当挖掘非最热店铺。\n";

		JSONObject body = new JSONObject();
		body.set("model", model.trim());
		JSONArray messages = new JSONArray();
		JSONObject msg = new JSONObject();
		msg.set("role", "user");
		msg.set("content", prompt);
		messages.add(msg);
		body.set("messages", messages);
		String resp = QianfanChatClient.newChatCompletionsRequest()
				.body(body.toString()).timeout(120000).execute().body();
		JSONObject respJson = new JSONObject(resp);
		String resultText = "";
		if (respJson.getJSONArray("choices") != null && respJson.getJSONArray("choices").size() > 0) {
			JSONObject choice0 = respJson.getJSONArray("choices").getJSONObject(0);
			if (choice0.getJSONObject("message") != null) {
				resultText = choice0.getJSONObject("message").getStr("content");
			}
		}
		if (resultText == null || resultText.isEmpty()) {
			return null;
		}
		Matcher m = Pattern.compile("\\d+").matcher(resultText);
		StringBuilder sb = new StringBuilder();
		while (m.find()) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(m.group());
		}
		return sb.length() > 0 ? sb.toString() : null;
	}

	private static String formatRecMetricsBrief(Object recMetricsObj) {
		if (!(recMetricsObj instanceof Map)) {
			return "";
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> rm = (Map<String, Object>) recMetricsObj;
		StringBuilder mb = new StringBuilder();
		for (Map.Entry<String, Object> e : rm.entrySet()) {
			if ("综合".equals(e.getKey())) {
				continue;
			}
			double v = 0;
			try {
				v = ((Number) e.getValue()).doubleValue();
			} catch (Exception ignored) {
				continue;
			}
			if (v > 0.005) {
				if (mb.length() > 0) {
					mb.append(",");
				}
				mb.append(e.getKey()).append(":").append(String.format("%.1f", v));
			}
		}
		return mb.length() == 0 ? "-" : mb.toString();
	}

	private String aiRecommendHint(String uid, Set<String> myTags, Set<String> myFavs, List<String> tagLabelList,
			String factorNarrative, List<Map<String, Object>> topShops) {
		String apiKey = Common.getProperty("qianfan_api_key");
		if (apiKey == null || apiKey.trim().isEmpty()) {
			return "";
		}
		String model = Common.getProperty("qianfan_model");
		if (model == null || model.trim().isEmpty()) {
			model = QianfanChatClient.DEFAULT_QIANFAN_MODEL;
		}
		String tagNamesHuman = (tagLabelList == null || tagLabelList.isEmpty()) ? "未设置" : String.join("、", tagLabelList);
		StringBuilder names = new StringBuilder();
		int n = Math.min(6, topShops.size());
		for (int i = 0; i < n; i++) {
			Map<String, Object> s = topShops.get(i);
			if (s.get("sname") != null) {
				names.append(s.get("sname").toString()).append("、");
			}
		}
		String prompt = "你是美食推荐助手。用一句话（不超过50字）向用户解释为何推荐这些店，口语化、不要编号、不要出现店铺id或数字id。\n"
				+ factorNarrative + "\n" + "用户口味偏好（中文标签名）:" + tagNamesHuman + "\n" + "推荐店名（按顺序）:" + names.toString()
				+ "\n可自然提到综合了距离、口碑、行为等因素之一，但不要列公式或技术细节。\n只输出这一句。";

		JSONObject body = new JSONObject();
		body.set("model", model.trim());
		JSONArray messages = new JSONArray();
		JSONObject msg = new JSONObject();
		msg.set("role", "user");
		msg.set("content", prompt);
		messages.add(msg);
		body.set("messages", messages);
		String resp = QianfanChatClient.newChatCompletionsRequest()
				.body(body.toString()).timeout(120000).execute().body();
		JSONObject respJson = new JSONObject(resp);
		String resultText = "";
		if (respJson.getJSONArray("choices") != null && respJson.getJSONArray("choices").size() > 0) {
			JSONObject choice0 = respJson.getJSONArray("choices").getJSONObject(0);
			if (choice0.getJSONObject("message") != null) {
				resultText = choice0.getJSONObject("message").getStr("content");
			}
		}
		return resultText == null ? "" : resultText.trim().replaceAll("[\r\n]+", "");
	}

	// ---------- 管理端 · 千帆数据运营（JSONP，供 dashbord / drill / shopreplaymap 调用） ----------

	private int parseAiOpsDays(String raw) {
		try {
			int d = Integer.parseInt(raw == null ? "30" : raw.trim());
			if (d < 1) {
				return 1;
			}
			if (d > 365) {
				return 365;
			}
			return d;
		} catch (Exception e) {
			return 30;
		}
	}

	private Integer parseSidOrNull(String raw) {
		if (raw == null || raw.trim().isEmpty()) {
			return null;
		}
		try {
			int v = Integer.parseInt(raw.trim());
			return v > 0 ? v : null;
		} catch (Exception e) {
			return null;
		}
	}

	private long safeLong(Object o) {
		if (o == null) {
			return 0L;
		}
		try {
			return ((Number) o).longValue();
		} catch (Exception e) {
			return 0L;
		}
	}

	private double safeDouble(Object o) {
		if (o == null) {
			return 0d;
		}
		try {
			return ((Number) o).doubleValue();
		} catch (Exception e) {
			return 0d;
		}
	}

	/**
	 * 聚合平台/单店客观指标，供大模型只做归纳与建议（勿编造未提供数字）。
	 */
	private Map<String, Object> buildPlatformFactMap(int days, Integer sidScope) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("windowDays", days);
		m.put("shopScopeId", sidScope);

		String billDateClause = " ndate >= DATE_SUB(NOW(), INTERVAL " + days + " DAY) ";
		String billSidClause = sidScope == null ? "" : (" and sid = " + sidScope + " ");

		List<Map<String, Object>> billAgg = databaseService.find(
				"select count(*) as cnt, coalesce(sum(cast(total as decimal(14,2))),0) as gmv from fs_bill where " + billDateClause + billSidClause);
		if (billAgg != null && !billAgg.isEmpty()) {
			Map<String, Object> r = billAgg.get(0);
			m.put("orderCountInWindow", safeLong(r.get("cnt")));
			m.put("gmvInWindow", safeDouble(r.get("gmv")));
		} else {
			m.put("orderCountInWindow", 0L);
			m.put("gmvInWindow", 0d);
		}

		List<Map<String, Object>> pend = databaseService.find(
				"select count(*) as c from fs_bill where " + billDateClause + billSidClause + " and state=" + BillState.PENDING_PAY);
		m.put("pendingPayOrders", pend != null && !pend.isEmpty() ? safeLong(pend.get(0).get("c")) : 0L);

		List<Map<String, Object>> shopCntRow = databaseService.find("select count(*) as c from fs_shop");
		m.put("shopTotal", shopCntRow != null && !shopCntRow.isEmpty() ? safeLong(shopCntRow.get(0).get("c")) : 0L);
		List<Map<String, Object>> pa = databaseService
				.find("select count(*) as c from fs_shop where state=" + ShopAuditState.PENDING);
		m.put("shopsPendingAudit", pa != null && !pa.isEmpty() ? safeLong(pa.get(0).get("c")) : 0L);
		List<Map<String, Object>> low = databaseService.find(
				"select count(*) as c from fs_shop where (pf+0) > 0 and (pf+0) < 4");
		m.put("shopsLowScoreUnder4", low != null && !low.isEmpty() ? safeLong(low.get(0).get("c")) : 0L);

		List<Map<String, Object>> goodsCnt = databaseService.find("select count(*) as c from fs_good");
		m.put("skuTotal", goodsCnt != null && !goodsCnt.isEmpty() ? safeLong(goodsCnt.get(0).get("c")) : 0L);

		List<Map<String, Object>> appeals = databaseService.find(
				"select count(*) as c from fs_replay_appeal where state=" + AppealState.PENDING);
		m.put("appealsPending", appeals != null && !appeals.isEmpty() ? safeLong(appeals.get(0).get("c")) : 0L);

		return m;
	}

	private Map<String, Object> buildShopFactMap(int sid, int days) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("shopId", sid);
		m.put("windowDays", days);

		List<Map<String, Object>> shopRow = databaseService.find("select id,sname,pf,state,typecn from fs_shop where id=" + sid);
		if (shopRow == null || shopRow.isEmpty()) {
			m.put("error", "店铺不存在");
			return m;
		}
		Map<String, Object> sh = shopRow.get(0);
		ShopSupport.enrichRow(sh);
		m.put("shopName", sh.get("sname") == null ? "" : sh.get("sname").toString());
		m.put("shopPf", sh.get("pf") == null ? "" : sh.get("pf").toString());
		m.put("shopStatecn", sh.get("statecn") == null ? "" : sh.get("statecn").toString());
		m.put("shopTypecn", sh.get("typecn") == null ? "" : sh.get("typecn").toString());

		String billClause = " sid=" + sid + " and ndate >= DATE_SUB(NOW(), INTERVAL " + days + " DAY) ";
		List<Map<String, Object>> billAgg = databaseService.find(
				"select count(*) as cnt, coalesce(sum(cast(total as decimal(14,2))),0) as gmv from fs_bill where " + billClause);
		if (billAgg != null && !billAgg.isEmpty()) {
			m.put("orderCountInWindow", safeLong(billAgg.get(0).get("cnt")));
			m.put("gmvInWindow", safeDouble(billAgg.get(0).get("gmv")));
		}

		List<Map<String, Object>> topGoods = databaseService.find(
				"select gname, ifnull(xl,0) as xl, ifnull(pf,'') as gpf from fs_good where sid=" + sid
						+ " order by (xl+0) desc limit 10");
		m.put("topGoodsBySales", topGoods == null ? Collections.emptyList() : topGoods);

		List<Map<String, Object>> slowGoods = databaseService.find(
				"select gname, ifnull(xl,0) as xl, ifnull(pf,'') as gpf from fs_good where sid=" + sid
						+ " and (xl is null or xl+0 < 3) order by (xl+0) asc limit 12");
		m.put("slowOrLowSalesGoods", slowGoods == null ? Collections.emptyList() : slowGoods);

		List<Map<String, Object>> skuCnt = databaseService.find("select count(*) as c from fs_good where sid=" + sid);
		m.put("skuCount", skuCnt != null && !skuCnt.isEmpty() ? safeLong(skuCnt.get(0).get("c")) : 0L);
		List<Map<String, Object>> unsoldCnt = databaseService.find(
				"select count(*) as c from fs_good where sid=" + sid + " and (xl is null or xl+0=0)");
		m.put("unsoldSkuCount", unsoldCnt != null && !unsoldCnt.isEmpty() ? safeLong(unsoldCnt.get(0).get("c")) : 0L);

		List<Map<String, Object>> blogCnt = databaseService.find("select count(*) as c from fs_blog where sid=" + sid);
		m.put("blogCount", blogCnt != null && !blogCnt.isEmpty() ? safeLong(blogCnt.get(0).get("c")) : 0L);
		List<Map<String, Object>> qaCnt = databaseService.find(
				"select count(*) as c from fs_shop_qa where sid=" + sid + " and qtype='Q'");
		m.put("qaQuestionCount", qaCnt != null && !qaCnt.isEmpty() ? safeLong(qaCnt.get(0).get("c")) : 0L);
		List<Map<String, Object>> couponSum = databaseService.find(
				"select coalesce(sum(cast(ifnull(total,0) as signed)),0) as s from fs_youhuiquan where sid=" + sid);
		m.put("couponTotalIssued", couponSum != null && !couponSum.isEmpty() ? safeLong(couponSum.get(0).get("s")) : 0L);
		List<Map<String, Object>> apCnt = databaseService.find(
				"select count(*) as c from fs_replay_appeal where sid=" + sid + " and state=" + AppealState.PENDING);
		m.put("appealsPendingForShop", apCnt != null && !apCnt.isEmpty() ? safeLong(apCnt.get(0).get("c")) : 0L);

		List<Map<String, Object>> replayPfs = databaseService.find(
				"select pf from fs_replay where type=9 and pid='" + sid + "'");
		int replayTotal = replayPfs == null ? 0 : replayPfs.size();
		int rpWithStar = 0;
		int rpGood = 0;
		if (replayPfs != null) {
			for (Map<String, Object> row : replayPfs) {
				Object po = row.get("pf");
				if (po == null || po.toString().trim().isEmpty()) {
					continue;
				}
				try {
					double pv = Double.parseDouble(po.toString().trim());
					rpWithStar++;
					if (pv >= 4.0) {
						rpGood++;
					}
				} catch (Exception ignored) {
				}
			}
		}
		m.put("userReplayCountTypeShop", (long) replayTotal);
		m.put("replayWithStarCount", (long) rpWithStar);
		m.put("goodReviewRatePercent",
				rpWithStar > 0 ? (int) Math.round(rpGood * 100.0 / rpWithStar) : null);

		return m;
	}

	/**
	 * 工作台一次聚合：待办计数、低分店铺清单（与 KPI 同源 SQL）、公告摘要，避免前端多次 list 全表。
	 */
	private Map<String, Object> buildWorkbenchSummaryMap() {
		Map<String, Object> m = new LinkedHashMap<>();

		List<Map<String, Object>> bp = databaseService
				.find("select count(*) as c from fs_bill where state=" + BillState.PENDING_PAY);
		m.put("billPendingPay", bp != null && !bp.isEmpty() ? safeLong(bp.get(0).get("c")) : 0L);

		List<Map<String, Object>> pa = databaseService
				.find("select count(*) as c from fs_shop where state=" + ShopAuditState.PENDING);
		m.put("shopPendingAudit", pa != null && !pa.isEmpty() ? safeLong(pa.get(0).get("c")) : 0L);

		List<Map<String, Object>> lowRows = databaseService.find(
				"select id, sname, pf from fs_shop where (pf+0) > 0 and (pf+0) < 4 order by (pf+0) asc");
		if (lowRows == null) {
			lowRows = Collections.emptyList();
		}
		m.put("shopLowPfCount", (long) lowRows.size());
		List<Map<String, Object>> lowPfShops = new ArrayList<>();
		for (Map<String, Object> row : lowRows) {
			Map<String, Object> one = new LinkedHashMap<>();
			one.put("id", row.get("id"));
			one.put("sname", row.get("sname") == null ? "" : row.get("sname").toString());
			one.put("pf", row.get("pf"));
			lowPfShops.add(one);
		}
		m.put("lowPfShops", lowPfShops);

		List<Map<String, Object>> pendPreview = databaseService.find(
				"select id, sname from fs_shop where state=" + ShopAuditState.PENDING + " order by id desc limit 3");
		m.put("pendingAuditPreview", pendPreview == null ? Collections.emptyList() : pendPreview);

		List<Map<String, Object>> ap = databaseService
				.find("select count(*) as c from fs_replay_appeal where state=" + AppealState.PENDING);
		m.put("appealsPending", ap != null && !ap.isEmpty() ? safeLong(ap.get(0).get("c")) : 0L);

		List<Map<String, Object>> cq = databaseService.find(
				"select count(*) as c from fs_youhuiquan where ndate >= date_sub(now(), interval 30 day) and (typeid='2' or typeid=2)");
		m.put("couponClaim30d", cq != null && !cq.isEmpty() ? safeLong(cq.get(0).get("c")) : 0L);
		List<Map<String, Object>> ct = databaseService.find(
				"select count(*) as c from fs_youhuiquan where ndate >= date_sub(now(), interval 30 day) and (typeid='1' or typeid=1)");
		m.put("couponTpl30d", ct != null && !ct.isEmpty() ? safeLong(ct.get(0).get("c")) : 0L);

		List<Map<String, Object>> notices = databaseService
				.find("select id,title,ndate,type,shop from fs_sysmsg order by id desc limit 6");
		m.put("latestSysmsg", notices == null ? Collections.emptyList() : notices);

		return m;
	}

	private static final String AI_ASSISTANT_SYSTEM = "你是「美食小程序」内置的 AI 小助手（管理端/商家端）。请用简体中文作答，语气友好、专业。"
			+ "若用户消息下方附有【系统提供的业务事实 JSON】，其中的数字与条目来自平台数据库切片：必须优先基于该 JSON 作答，禁止编造 JSON 未出现的订单金额、销量、店铺名、用户数等。"
			+ "当用户问题涉及运营、审核、订单、店铺评分、库存、优惠等业务指标且 JSON 提供了相关数据时，不要只给一句话结论；请尽量采用清晰结构（可用简短小标题）：①「结论摘要」2～4 句；②「数据解读」——说明关键数字含义、对比或趋势（仅基于 JSON 已有字段推断，勿臆测）；③「建议与下一步」——列出 2～5 条可执行建议，使用「建议」「可考虑」「宜关注」等措辞，避免命令式或违法承诺；④「提示」——若有数据盲区、权限限制或需人工复核的点，单列简要说明。全文建议控制在约 900 字以内，条目精炼。"
			+ "若为简单寒暄、功能说明或与 JSON 无关的通用问题，可简短回答，不必强行套用上述结构。"
			+ "若 JSON 中含 nearbyShopsKm（含 sname、distanceKm），附近美食类问题只能推荐数组内店铺并标注距离，禁止编造店名；若无定位或数组为空，如实说明。"
			+ "不提供医疗诊断、投资理财、违法操作建议；退款与纠纷请引导按订单流程或联系平台客服。";

	private String clipAssistantQuestion(String s) {
		if (s == null) {
			return "";
		}
		s = s.trim();
		if (s.length() <= 1500) {
			return s;
		}
		return s.substring(0, 1500) + "…";
	}

	private String clipAssistantFactsJson(String json) {
		if (json == null) {
			return "{}";
		}
		if (json.length() <= 4000) {
			return json;
		}
		return json.substring(0, 4000);
	}

	/**
	 * 为助手拼装一小段事实 JSON：商家带 sid 时用单店事实；管理员/运营/审核 roletype 1/5/6 用平台聚合；
	 * 普通用户带 uid 时用个人订单/评价计数。控制长度避免超 token。
	 */
	private String buildAssistantFactsJson(HttpServletRequest req) {
		try {
			Integer sid = parseSidOrNull(req.getParameter("sid"));
			String rt = req.getParameter("roletype");
			if (rt != null) {
				rt = rt.trim();
			} else {
				rt = "";
			}
			Integer uid = parseSidOrNull(req.getParameter("uid"));

			if (sid != null && sid > 0) {
				Map<String, Object> facts = buildShopFactMap(sid, 14);
				if (facts.containsKey("error")) {
					Map<String, Object> err = new LinkedHashMap<>();
					err.put("shopScope", sid);
					err.put("error", facts.get("error"));
					return clipAssistantFactsJson(JSON.toJSONString(err));
				}
				facts.remove("topGoodsBySales");
				facts.remove("slowOrLowSalesGoods");
				return clipAssistantFactsJson(JSON.toJSONString(facts));
			}
			if ("1".equals(rt) || "5".equals(rt) || "6".equals(rt)) {
				Map<String, Object> facts = buildPlatformFactMap(14, null);
				return clipAssistantFactsJson(JSON.toJSONString(facts));
			}
			if (uid != null && uid > 0) {
				Map<String, Object> u = new LinkedHashMap<>();
				u.put("role", "user");
				u.put("userId", uid);
				List<Map<String, Object>> b = databaseService.find(
						"select count(*) as c from fs_bill where uid=" + uid
								+ " and str_to_date(ndate,'%Y-%m-%d %H:%i:%s') >= date_sub(now(), interval 30 day)");
				u.put("ordersLast30d", b != null && !b.isEmpty() ? safeLong(b.get(0).get("c")) : 0L);
				List<Map<String, Object>> r = databaseService
						.find("select count(*) as c from fs_replay where uid=" + uid);
				u.put("replayTotal", r != null && !r.isEmpty() ? safeLong(r.get(0).get("c")) : 0L);
				double userLat = parseDoubleParam(req.getParameter("lat"), Double.NaN);
				double userLng = parseDoubleParam(req.getParameter("lng"), Double.NaN);
				if (!Double.isNaN(userLat) && !Double.isNaN(userLng)) {
					u.put("userLat", userLat);
					u.put("userLng", userLng);
					List<Map<String, Object>> near = buildNearbyShopsPreview(userLat, userLng, 10);
					u.put("nearbyShopsKm", near);
					if (near.isEmpty()) {
						u.put("nearbyNote", "已传定位但库内暂无带有效坐标的审核通过店铺，无法列出距离");
					}
				} else {
					u.put("locationNote", "未传经纬度 lat/lng，无法计算附近店铺；用户需在小程序侧授权定位并在请求中附带坐标");
				}
				return clipAssistantFactsJson(JSON.toJSONString(u));
			}
		} catch (Exception ignored) {
		}
		return "{\"note\":\"未携带店铺/用户/管理员上下文，仅做通用问答\"}";
	}

	/**
	 * 简单问答小助手（JSON）。可选 query：q、roletype、sid（商家）、uid（用户）；普通用户传 lat、lng（与小程序 getLocation 一致，建议 gcj02）时在事实 JSON 中附带 nearbyShopsKm 供附近推荐类回答。
	 */
	@RequestMapping(value = "/aiAssistantChat", produces = "application/json; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> aiAssistantChat(HttpServletRequest req) {
		Map<String, Object> out = new LinkedHashMap<>();
		String q = clipAssistantQuestion(req.getParameter("q"));
		if (q.isEmpty()) {
			out.put("ok", false);
			out.put("error", "请输入问题");
			return out;
		}
		try {
			String facts = buildAssistantFactsJson(req);
			boolean hasRichContext = facts != null && !facts.contains("未携带店铺/用户/管理员上下文");
			String userMsg = "【用户问题】\n" + q + "\n\n【系统提供的业务事实 JSON】\n" + facts;
			String reply = QianfanChatClient.chat(AI_ASSISTANT_SYSTEM, userMsg);
			out.put("ok", true);
			out.put("reply", reply);
			out.put("hasContext", hasRichContext);
		} catch (Exception e) {
			out.put("ok", false);
			out.put("error", e.getMessage() == null ? "unknown" : e.getMessage());
		}
		return out;
	}

	@RequestMapping(value = "/aiAssistantChatJ", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
	@ResponseBody
	public String aiAssistantChatJ(HttpServletRequest req) {
		return renderJsonp(aiAssistantChat(req), req);
	}

	private static final String AI_OPS_SYSTEM = "你是本地美食电商管理后台的数据运营顾问，面向平台管理员输出中文。"
			+ "必须严格遵守：只能使用用户消息中「事实数据」里出现的数字与条目做分析，不得编造未提供的销量、评分、订单量或店铺名。"
			+ "对低销量、低评分商品用「建议评估是否优化或下架」等表述，避免绝对化医疗或违法承诺。"
			+ "输出结构清晰，使用简短小节与条目，总字数控制在 800 字以内。";

	private static final String AI_SHOP_DECISION_SYSTEM = "你是本地美食电商平台「平台运营中心」顾问，面向单个商家撰写将推送至商家小程序系统消息的中文「运营决策参考」。"
			+ "必须仅用事实数据中的数字与条目，禁止编造订单、评价、券发放量。语气专业、可执行；对整改类用「建议」「可评估」等表述，避免命令式违法承诺。"
			+ "建议须覆盖：菜品/SKU 结构（滞销与热销）、定价与套餐、优惠券与活动节奏、店铺口碑与口味/服务体验、待处理申诉与客诉优先级等中的相关项（无数据的维度可简要提示加强数据沉淀）。"
			+ "输出结构：先 2～4 句结论摘要；再分条列出「决策建议」（每条前可加 1、2、3）；最后一句风险提示。总字数 600～1000 字，不要用 Markdown # 标题符号。";

	@RequestMapping(value = "/aiOpsPlatformBriefJ", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
	@ResponseBody
	public String aiOpsPlatformBriefJ(HttpServletRequest req) {
		Map<String, Object> out = new LinkedHashMap<>();
		try {
			int days = parseAiOpsDays(req.getParameter("days"));
			Integer sidScope = parseSidOrNull(req.getParameter("sid"));
			Map<String, Object> facts = buildPlatformFactMap(days, sidScope);
			boolean nocache = "1".equals(req.getParameter("nocache")) || "true".equalsIgnoreCase(req.getParameter("nocache"));
			String cacheKey = "pb|" + days + "|" + (sidScope == null ? "all" : String.valueOf(sidScope));
			if (!nocache) {
				String hit = aiOpsCacheGet(cacheKey);
				if (hit != null) {
					out.put("ok", true);
					out.put("text", hit);
					out.put("facts", facts);
					out.put("fromCache", true);
					return renderJsonp(out, req);
				}
			}
			String factsJson = JSON.toJSONString(facts);
			String scopeHint = sidScope == null ? "全平台（订单统计已按筛选店铺维度若传 sid）" : ("当前仅统计店铺ID=" + sidScope + " 的订单与平台全局供给概况");
			String userMsg = "【分析任务】基于事实数据生成平台运营简报（结论 + 可执行建议 + 风险点）。\n【范围说明】" + scopeHint
					+ "\n【事实数据 JSON】\n" + factsJson;
			String text = QianfanChatClient.chat(AI_OPS_SYSTEM, userMsg);
			if (!nocache) {
				aiOpsCachePut(cacheKey, text);
			}
			out.put("ok", true);
			out.put("text", text);
			out.put("facts", facts);
			out.put("fromCache", false);
		} catch (Exception e) {
			out.put("ok", false);
			out.put("error", e.getMessage() == null ? "unknown" : e.getMessage());
		}
		return renderJsonp(out, req);
	}

	@RequestMapping(value = "/aiOpsDrillInsightJ", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
	@ResponseBody
	public String aiOpsDrillInsightJ(HttpServletRequest req) {
		Map<String, Object> out = new LinkedHashMap<>();
		try {
			int days = parseAiOpsDays(req.getParameter("days"));
			Integer sidScope = parseSidOrNull(req.getParameter("sid"));
			String domain = req.getParameter("domain");
			if (domain == null) {
				domain = "";
			}
			domain = domain.replaceAll("[^a-zA-Z0-9_\\-]", "");
			if (domain.length() > 24) {
				domain = domain.substring(0, 24);
			}
			if (domain.isEmpty()) {
				domain = "biz";
			}

			String digest = req.getParameter("digest");
			if (digest == null) {
				digest = "";
			}
			if (digest.length() > 4000) {
				digest = digest.substring(0, 4000);
			}

			Map<String, Object> facts = buildPlatformFactMap(days, sidScope);
			String factsJson = JSON.toJSONString(facts);
			String userMsg = "【分析任务】当前钻取分析域：" + domain + "。页面已根据规则生成「结论摘要与指标文本」，请你在此基础上做深度解读："
					+ "说明可能业务含义、关注异常、下一步可采取的运营动作；不要重复编造数字。\n"
					+ "【本页摘要与指标（文本）】\n" + digest + "\n\n【同期平台事实 JSON】\n" + factsJson;
			String text = QianfanChatClient.chat(AI_OPS_SYSTEM, userMsg);
			out.put("ok", true);
			out.put("text", text);
			out.put("facts", facts);
		} catch (Exception e) {
			out.put("ok", false);
			out.put("error", e.getMessage() == null ? "unknown" : e.getMessage());
		}
		return renderJsonp(out, req);
	}

	@RequestMapping(value = "/aiOpsShopAdviceJ", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
	@ResponseBody
	public String aiOpsShopAdviceJ(HttpServletRequest req) {
		Map<String, Object> out = new LinkedHashMap<>();
		try {
			Integer sid = parseSidOrNull(req.getParameter("sid"));
			if (sid == null) {
				out.put("ok", false);
				out.put("error", "缺少有效 sid");
				return renderJsonp(out, req);
			}
			int days = parseAiOpsDays(req.getParameter("days"));
			Map<String, Object> facts = buildShopFactMap(sid, days);
			if (facts.containsKey("error")) {
				out.put("ok", false);
				out.put("error", facts.get("error"));
				return renderJsonp(out, req);
			}
			String factsJson = JSON.toJSONString(facts);
			String mode = req.getParameter("mode");
			if (mode == null) {
				mode = "ops";
			}
			mode = mode.trim().toLowerCase();
			String digest = req.getParameter("digest");
			if (digest == null) {
				digest = "";
			}
			if (digest.length() > 2500) {
				digest = digest.substring(0, 2500);
			}
			boolean nocache = "1".equals(req.getParameter("nocache")) || "true".equalsIgnoreCase(req.getParameter("nocache"));
			int digestTag = digest.isEmpty() ? 0 : digest.hashCode();
			String saCacheKey = "sa|" + sid + "|" + days + "|" + mode + "|" + digestTag;
			if (!nocache) {
				String hit = aiOpsCacheGet(saCacheKey);
				if (hit != null) {
					out.put("ok", true);
					out.put("text", hit);
					out.put("facts", facts);
					out.put("fromCache", true);
					return renderJsonp(out, req);
				}
			}
			String text;
			if ("decision".equals(mode)) {
				String userMsg = "【任务】请根据以下事实 JSON，为该店生成「运营决策参考」全文（将推送给商家）。若下列「页面摘要」与 JSON 一致可互相印证；若有冲突以 JSON 为准。\n"
						+ "【页面摘要（可选）】\n" + digest + "\n\n【事实 JSON】\n" + factsJson;
				text = QianfanChatClient.chat(AI_SHOP_DECISION_SYSTEM, userMsg);
			} else {
				String userMsg = "【任务】你是平台方，为该商家写一份「运营建议」草稿，将推送到商家小程序系统消息。"
						+ "语气专业、建设性；对高销量高口碑菜品可建议保持品质并适度增加曝光；对长期低销量或评分偏低的菜品建议优化图文、定价或考虑下架，用「建议评估」表述。"
						+ "结合店铺评分与订单情况给出整改或鼓励方向。控制在 500 字内，不要使用 Markdown 标题符号。\n【事实 JSON】\n" + factsJson;
				text = QianfanChatClient.chat(AI_OPS_SYSTEM, userMsg);
			}
			if (!nocache) {
				aiOpsCachePut(saCacheKey, text);
			}
			out.put("ok", true);
			out.put("text", text);
			out.put("facts", facts);
			out.put("fromCache", false);
		} catch (Exception e) {
			out.put("ok", false);
			out.put("error", e.getMessage() == null ? "unknown" : e.getMessage());
		}
		return renderJsonp(out, req);
	}

	/**
	 * 工作台聚合摘要（JSONP）：与 KPI / 待办 / 风险列表同源，一次请求替代多次全表 list。
	 */
	@RequestMapping(value = "/opsWorkbenchSummaryJ", produces = "text/plain; charset=utf-8", method = { RequestMethod.GET,
			RequestMethod.POST })
	@ResponseBody
	public String opsWorkbenchSummaryJ(HttpServletRequest req) {
		Map<String, Object> out = new LinkedHashMap<>();
		try {
			out.put("ok", true);
			out.put("summary", buildWorkbenchSummaryMap());
		} catch (Exception e) {
			out.put("ok", false);
			out.put("error", e.getMessage() == null ? "unknown" : e.getMessage());
		}
		return renderJsonp(out, req);
	}

	/**
	 * 商家端运营数据分析（准确销量口径：基于订单 fs_bill.gids + gnames 中 *数量 汇总）。
	 * - days：统计窗口（默认30，1~365）
	 * - 仅统计已付款/已完成订单
	 * 返回 JSON（非 JSONP），供小程序商家端使用。
	 */
	@RequestMapping(value = "/merchantOpsReport", produces = "application/json; charset=utf-8", method = {
			RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> merchantOpsReport(HttpServletRequest req) {
		Map<String, Object> out = new LinkedHashMap<>();
		try {
			Integer sid = parseSidOrNull(req.getParameter("sid"));
			if (sid == null) {
				out.put("ok", false);
				out.put("error", "缺少有效 sid");
				return out;
			}
			int days = parseAiOpsDays(req.getParameter("days"));

			// 1) 拉取本店商品（用于名称、价格兜底）
			List<Map<String, Object>> goods = databaseService
					.find("select id,gname,price,xl from fs_good where sid=" + sid);
			Map<Integer, Map<String, Object>> goodById = new HashMap<>();
			if (goods != null) {
				for (Map<String, Object> g : goods) {
					Integer gid = null;
					try {
						gid = g.get("id") == null ? null : ((Number) g.get("id")).intValue();
					} catch (Exception ignored) {
					}
					if (gid != null) {
						goodById.put(gid, g);
					}
				}
			}

			// 2) 拉取本店窗口内订单
			String billSql = "select id,gids,gnames,total,ndate,state,uid,way,yhqid from fs_bill where sid="
					+ sid + " and ndate >= date_sub(now(), interval " + days + " day)";
			List<Map<String, Object>> bills = databaseService.find(billSql);
			if (bills == null) {
				bills = Collections.emptyList();
			}

			// 3) 汇总（只统计已付款/已完成）
			long paidOrderCnt = 0;
			double paidGmv = 0d;
			long paidItemCnt = 0;
			Set<String> paidBuyerSet = new HashSet<>();
			Map<String, Map<String, Object>> trend = new LinkedHashMap<>();
			Map<Integer, Long> goodQtyMap = new HashMap<>();

			for (Map<String, Object> b : bills) {
				if (!isPaidBillState(b)) {
					continue;
				}
				paidOrderCnt++;
				String uid = b.get("uid") == null ? "" : b.get("uid").toString();
				if (!uid.isEmpty()) {
					paidBuyerSet.add(uid);
				}
				double total = parseDoubleSafe(b.get("total") == null ? "" : b.get("total").toString(), 0d);
				if (total > 0) {
					paidGmv += total;
				}
				String day = dayOfNdate(b.get("ndate"));
				if (day.isEmpty()) {
					day = "-";
				}
				Map<String, Object> td = trend.get(day);
				if (td == null) {
					td = new LinkedHashMap<>();
					td.put("day", day);
					td.put("paidOrders", 0L);
					td.put("gmv", 0d);
					td.put("items", 0L);
					td.put("buyers", new HashSet<String>());
					trend.put(day, td);
				}
				td.put("paidOrders", ((Long) td.get("paidOrders")) + 1L);
				td.put("gmv", ((Double) td.get("gmv")) + total);
				@SuppressWarnings("unchecked")
				Set<String> buyers = (Set<String>) td.get("buyers");
				if (!uid.isEmpty()) {
					buyers.add(uid);
				}

				String gidsCsv = b.get("gids") == null ? "" : b.get("gids").toString();
				String gnamesCsv = b.get("gnames") == null ? "" : b.get("gnames").toString();
				List<String> gids = splitCsv(gidsCsv);
				List<Integer> counts = parseBillCounts(gidsCsv, gnamesCsv);
				for (int i = 0; i < Math.min(gids.size(), counts.size()); i++) {
					Integer gid = parseSidOrNull(gids.get(i));
					if (gid == null) {
						continue;
					}
					int c = counts.get(i) == null ? 1 : counts.get(i);
					if (c < 1) {
						c = 1;
					}
					paidItemCnt += c;
					td.put("items", ((Long) td.get("items")) + c);
					Long old = goodQtyMap.get(gid);
					goodQtyMap.put(gid, (old == null ? 0L : old) + c);
				}
			}

			// 4) trend 输出（buyers set -> buyers count），并按日期升序
			List<Map<String, Object>> trendList = new ArrayList<>();
			for (Map<String, Object> td : trend.values()) {
				@SuppressWarnings("unchecked")
				Set<String> buyers = (Set<String>) td.get("buyers");
				td.put("buyerCount", buyers == null ? 0 : buyers.size());
				td.remove("buyers");
				trendList.add(td);
			}
			trendList.sort(Comparator.comparing(a -> (a.get("day") == null ? "" : a.get("day").toString())));

			// 5) Top 商品：按订单汇总销量（准确口径），返回 gname/price 兜底
			List<Map<String, Object>> goodSales = new ArrayList<>();
			if (goods != null) {
				for (Map<String, Object> g : goods) {
					Integer gid = null;
					try {
						gid = g.get("id") == null ? null : ((Number) g.get("id")).intValue();
					} catch (Exception ignored) {
					}
					if (gid == null) {
						continue;
					}
					long qty = goodQtyMap.get(gid) == null ? 0L : goodQtyMap.get(gid);
					Map<String, Object> row = new LinkedHashMap<>();
					row.put("id", gid);
					row.put("qty", qty);
					row.put("gname", g.get("gname") == null ? ("#" + gid) : g.get("gname"));
					row.put("price", g.get("price"));
					goodSales.add(row);
				}
			}

			List<Map<String, Object>> topGoods = goodSales.stream()
					.filter(r -> {
						Object q = r.get("qty");
						try {
							return q != null && ((Number) q).longValue() > 0;
						} catch (Exception e) {
							return false;
						}
					})
					.sorted((a, b) -> {
						long aq = ((Number) a.get("qty")).longValue();
						long bq = ((Number) b.get("qty")).longValue();
						return Long.compare(bq, aq);
					})
					.limit(5)
					.collect(Collectors.toList());

			Map<String, Object> summary = new LinkedHashMap<>();
			summary.put("sid", sid);
			summary.put("windowDays", days);
			summary.put("paidOrderCount", paidOrderCnt);
			summary.put("paidGmv", Math.round(paidGmv * 100.0) / 100.0);
			summary.put("paidItemCount", paidItemCnt);
			summary.put("buyerCount", (long) paidBuyerSet.size());
			summary.put("avgOrderValue", paidOrderCnt > 0 ? (Math.round((paidGmv / paidOrderCnt) * 100.0) / 100.0) : 0d);

			// 对比口径说明：goods.xl 是历史字段，可能未随订单同步更新
			long goodXlSum = 0;
			if (goods != null) {
				for (Map<String, Object> g : goods) {
					Object xl = g.get("xl");
					try {
						goodXlSum += xl == null ? 0 : ((Number) xl).longValue();
					} catch (Exception ignored) {
					}
				}
			}
			summary.put("legacyGoodXlSum", goodXlSum);

			out.put("ok", true);
			out.put("summary", summary);
			out.put("trendDaily", trendList);
			out.put("goodSales", goodSales);
			out.put("topGoods", topGoods);
		} catch (Exception e) {
			out.put("ok", false);
			out.put("error", e.getMessage() == null ? "unknown" : e.getMessage());
		}
		return out;
	}

	@RequestMapping(value = "/shopWordCloud", produces = "application/json; charset=utf-8", method = {
			RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> shopWordCloud(HttpServletRequest req) {
		Map<String, Object> out = new LinkedHashMap<>();
		try {
			Integer sid = parseSidOrNull(req.getParameter("sid"));
			if (sid == null) {
				out.put("ok", false);
				out.put("error", "缺少有效 sid");
				return out;
			}
			int days = normalizeWordCloudDays(req.getParameter("days"));
			int topN = normalizeWordCloudTopN(req.getParameter("topN"));
			int minFreq = normalizeWordCloudMinFreq(req.getParameter("minFreq"));
			boolean force = "1".equals(req.getParameter("force")) || "true".equalsIgnoreCase(req.getParameter("force"));
			Map<String, Object> data = queryShopWordCloud(sid, days, topN, minFreq, force);
			out.put("ok", true);
			out.putAll(data);
		} catch (Exception e) {
			out.put("ok", false);
			out.put("error", e.getMessage() == null ? "unknown" : e.getMessage());
		}
		return out;
	}

	@RequestMapping(value = "/rebuildShopWordCloud", produces = "application/json; charset=utf-8", method = {
			RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> rebuildShopWordCloud(HttpServletRequest req) {
		Map<String, Object> out = new LinkedHashMap<>();
		try {
			Integer sid = parseSidOrNull(req.getParameter("sid"));
			if (sid == null) {
				out.put("ok", false);
				out.put("error", "缺少有效 sid");
				return out;
			}
			int days = normalizeWordCloudDays(req.getParameter("days"));
			int topN = normalizeWordCloudTopN(req.getParameter("topN"));
			int minFreq = normalizeWordCloudMinFreq(req.getParameter("minFreq"));
			Map<String, Object> data = queryShopWordCloud(sid, days, topN, minFreq, true);
			out.put("ok", true);
			out.putAll(data);
		} catch (Exception e) {
			out.put("ok", false);
			out.put("error", e.getMessage() == null ? "unknown" : e.getMessage());
		}
		return out;
	}

	@RequestMapping(value = "/goodWordCloud", produces = "application/json; charset=utf-8", method = {
			RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> goodWordCloud(HttpServletRequest req) {
		Map<String, Object> out = new LinkedHashMap<>();
		try {
			Integer gid = parseSidOrNull(req.getParameter("gid"));
			if (gid == null) {
				out.put("ok", false);
				out.put("error", "缺少有效 gid");
				return out;
			}
			int days = normalizeWordCloudDays(req.getParameter("days"));
			int topN = normalizeWordCloudTopN(req.getParameter("topN"));
			int minFreq = normalizeWordCloudMinFreq(req.getParameter("minFreq"));
			boolean force = "1".equals(req.getParameter("force")) || "true".equalsIgnoreCase(req.getParameter("force"));
			Map<String, Object> data = queryGoodWordCloud(gid, days, topN, minFreq, force);
			out.put("ok", true);
			out.putAll(data);
		} catch (Exception e) {
			out.put("ok", false);
			out.put("error", e.getMessage() == null ? "unknown" : e.getMessage());
		}
		return out;
	}

	@RequestMapping(value = "/rebuildGoodWordCloud", produces = "application/json; charset=utf-8", method = {
			RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> rebuildGoodWordCloud(HttpServletRequest req) {
		Map<String, Object> out = new LinkedHashMap<>();
		try {
			Integer gid = parseSidOrNull(req.getParameter("gid"));
			if (gid == null) {
				out.put("ok", false);
				out.put("error", "缺少有效 gid");
				return out;
			}
			int days = normalizeWordCloudDays(req.getParameter("days"));
			int topN = normalizeWordCloudTopN(req.getParameter("topN"));
			int minFreq = normalizeWordCloudMinFreq(req.getParameter("minFreq"));
			Map<String, Object> data = queryGoodWordCloud(gid, days, topN, minFreq, true);
			out.put("ok", true);
			out.putAll(data);
		} catch (Exception e) {
			out.put("ok", false);
			out.put("error", e.getMessage() == null ? "unknown" : e.getMessage());
		}
		return out;
	}

	private Map<String, Object> queryGoodWordCloud(int gid, int days, int topN, int minFreq, boolean force) {
		Map<String, Object> out = new LinkedHashMap<>();
		Map<String, Object> cache = force ? null : loadGoodWordCloudCache(gid, days);
		if (cache != null && !isWordCloudCacheExpired(cache)) {
			out.put("gid", gid);
			out.put("days", days);
			out.put("sourceCount", parseIntObj(cache.get("source_count"), 0));
			out.put("updatedAt", cache.get("updated_at") == null ? "" : cache.get("updated_at").toString());
			out.put("items", parseItemsJson(cache.get("words_json")));
			out.put("fromCache", true);
			return out;
		}
		return refreshGoodWordCloudCache(gid, days, topN, minFreq);
	}

	private Map<String, Object> refreshGoodWordCloudCache(int gid, int days, int topN, int minFreq) {
		Map<String, Object> out = new LinkedHashMap<>();
		List<Map<String, Object>> rows = queryGoodReplayForWordCloud(gid, days);
		Set<String> stopWords = loadWordCloudStopWords();
		Map<String, WordStat> stats = buildWordStats(rows, stopWords);
		List<Map<String, Object>> items = toWordCloudItems(stats, minFreq, topN);
		String wordsJson = JSON.toJSONString(items);
		upsertGoodWordCloudCache(gid, days, rows.size(), wordsJson);
		out.put("gid", gid);
		out.put("days", days);
		out.put("sourceCount", rows.size());
		out.put("updatedAt", "");
		out.put("items", items);
		out.put("fromCache", false);
		return out;
	}

	private List<Map<String, Object>> queryGoodReplayForWordCloud(int gid, int days) {
		String sql = "select note,pf from fs_replay where type=1 and pid='" + gid
				+ "' and note is not null and trim(note)<>'' and str_to_date(ndate,'%Y-%m-%d %H:%i:%s') >= date_sub(now(), interval "
				+ days + " day)";
		List<Map<String, Object>> rows = databaseService.find(sql);
		return rows == null ? Collections.emptyList() : rows;
	}

	private Map<String, Object> loadGoodWordCloudCache(int gid, int days) {
		try {
			List<Map<String, Object>> rows = databaseService.find(
					"select gid,days,source_count,words_json,updated_at from fs_good_wordcloud_cache where gid=" + gid + " and days=" + days
							+ " limit 1");
			if (rows != null && !rows.isEmpty()) {
				return rows.get(0);
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	private void upsertGoodWordCloudCache(int gid, int days, int sourceCount, String wordsJson) {
		String json = wordsJson == null ? "[]" : escapeSql(wordsJson);
		try {
			databaseService.executeAction(
					"insert into fs_good_wordcloud_cache(gid,days,source_count,words_json,updated_at) values(" + gid + "," + days + ","
							+ sourceCount + ",'" + json
							+ "',now()) on duplicate key update source_count=values(source_count), words_json=values(words_json), updated_at=now()");
		} catch (Exception ignored) {
		}
	}

	private int normalizeWordCloudDays(String raw) {
		int d = parseIntSafe(raw, 30);
		if (d < 1) {
			return 1;
		}
		if (d > 365) {
			return 365;
		}
		return d;
	}

	private int normalizeWordCloudTopN(String raw) {
		int n = parseIntSafe(raw, 60);
		if (n < 10) {
			return 10;
		}
		if (n > 200) {
			return 200;
		}
		return n;
	}

	private int normalizeWordCloudMinFreq(String raw) {
		int n = parseIntSafe(raw, 2);
		if (n < 1) {
			return 1;
		}
		if (n > 10) {
			return 10;
		}
		return n;
	}

	private Map<String, Object> queryShopWordCloud(int sid, int days, int topN, int minFreq, boolean force) {
		Map<String, Object> out = new LinkedHashMap<>();
		Map<String, Object> cache = force ? null : loadShopWordCloudCache(sid, days);
		if (cache != null && !isWordCloudCacheExpired(cache)) {
			out.put("sid", sid);
			out.put("days", days);
			out.put("sourceCount", parseIntObj(cache.get("source_count"), 0));
			out.put("updatedAt", cache.get("updated_at") == null ? "" : cache.get("updated_at").toString());
			out.put("items", parseItemsJson(cache.get("words_json")));
			out.put("fromCache", true);
			return out;
		}
		return refreshShopWordCloudCache(sid, days, topN, minFreq);
	}

	private Map<String, Object> refreshShopWordCloudCache(int sid, int days, int topN, int minFreq) {
		Map<String, Object> out = new LinkedHashMap<>();
		List<Map<String, Object>> rows = queryShopReplayForWordCloud(sid, days);
		Set<String> stopWords = loadWordCloudStopWords();
		Map<String, WordStat> stats = buildWordStats(rows, stopWords);
		List<Map<String, Object>> items = toWordCloudItems(stats, minFreq, topN);
		String wordsJson = JSON.toJSONString(items);
		upsertShopWordCloudCache(sid, days, rows.size(), wordsJson);
		out.put("sid", sid);
		out.put("days", days);
		out.put("sourceCount", rows.size());
		out.put("updatedAt", "");
		out.put("items", items);
		out.put("fromCache", false);
		return out;
	}

	private List<Map<String, Object>> queryShopReplayForWordCloud(int sid, int days) {
		String sql = "select r.note as note, r.pf as pf from fs_replay r "
				+ "left join fs_good g on r.type=1 and cast(r.pid as unsigned)=g.id "
				+ "where r.type=1 and g.sid is not null and g.sid='" + sid
				+ "' and r.note is not null and trim(r.note)<>'' and str_to_date(r.ndate,'%Y-%m-%d %H:%i:%s') >= date_sub(now(), interval "
				+ days + " day)";
		List<Map<String, Object>> rows = databaseService.find(sql);
		return rows == null ? Collections.emptyList() : rows;
	}

	private Set<String> loadWordCloudStopWords() {
		Set<String> stopWords = new HashSet<>(BUILTIN_WORDCLOUD_STOP_WORDS);
		try {
			List<Map<String, Object>> rows = databaseService.find("select word from fs_word_stop where enabled=1");
			if (rows != null) {
				for (Map<String, Object> row : rows) {
					String w = row == null || row.get("word") == null ? "" : row.get("word").toString().trim();
					if (!w.isEmpty()) {
						stopWords.add(w);
					}
				}
			}
		} catch (Exception ignored) {
		}
		return stopWords;
	}

	private String normalizeReplayText(String raw) {
		if (raw == null) {
			return "";
		}
		String s = raw;
		s = HTML_TAG_PATTERN.matcher(s).replaceAll(" ");
		s = URL_PATTERN.matcher(s).replaceAll(" ");
		s = NON_WORD_PATTERN.matcher(s).replaceAll(" ");
		s = MULTI_SPACE_PATTERN.matcher(s).replaceAll(" ").trim();
		return s;
	}

	private List<String> tokenizeForWordCloud(String text) {
		if (WORDCLOUD_JIEBA != null) {
			return tokenizeForWordCloudJieba(text);
		}
		return tokenizeForWordCloudLegacy(text);
	}

	private List<String> tokenizeForWordCloudJieba(String text) {
		List<String> out = new ArrayList<>();
		if (text == null || text.isEmpty()) {
			return out;
		}
		try {
			for (SegToken st : WORDCLOUD_JIEBA.process(text, JiebaSegmenter.SegMode.SEARCH)) {
				if (st == null || st.word == null) {
					continue;
				}
				String w = st.word.trim();
				if (w.isEmpty()) {
					continue;
				}
				if (w.matches(".*[\\u4e00-\\u9fa5].*")) {
					out.add(w);
				} else {
					out.add(w.toLowerCase());
				}
			}
		} catch (Throwable t) {
			return tokenizeForWordCloudLegacy(text);
		}
		return out;
	}

	/** 无结巴库时的回退：按空格 + 中文 n-gram（易产生碎词，仅作兜底） */
	private List<String> tokenizeForWordCloudLegacy(String text) {
		List<String> out = new ArrayList<>();
		if (text == null || text.isEmpty()) {
			return out;
		}
		String[] parts = text.split(" ");
		for (String part : parts) {
			String p = part == null ? "" : part.trim();
			if (p.isEmpty()) {
				continue;
			}
			if (p.matches(".*[\\u4e00-\\u9fa5].*")) {
				int len = p.length();
				if (len <= 4) {
					out.add(p);
				} else {
					for (int n = 2; n <= 4; n++) {
						if (len < n) {
							continue;
						}
						for (int i = 0; i + n <= len; i++) {
							out.add(p.substring(i, i + n));
						}
					}
				}
			} else {
				out.add(p.toLowerCase());
			}
		}
		return out;
	}

	private boolean isLowQualityWord(String w) {
		if (w == null) {
			return true;
		}
		String t = w.trim();
		if (t.isEmpty()) {
			return true;
		}
		if (LOW_QUALITY_WORD_PARTS.contains(t)) {
			return true;
		}
		// 连续相同字符（如 哈哈哈、啊啊啊）
		int same = 1;
		for (int i = 1; i < t.length(); i++) {
			if (t.charAt(i) == t.charAt(i - 1)) {
				same++;
				if (same >= 3) {
					return true;
				}
			} else {
				same = 1;
			}
		}
		return false;
	}

	private Map<String, WordStat> buildWordStats(List<Map<String, Object>> rows, Set<String> stopWords) {
		Map<String, WordStat> stats = new HashMap<>();
		if (rows == null || rows.isEmpty()) {
			return stats;
		}
		for (Map<String, Object> row : rows) {
			String note = row == null || row.get("note") == null ? "" : row.get("note").toString();
			String text = normalizeReplayText(note);
			if (text.isEmpty()) {
				continue;
			}
			double pf = parseDoubleSafe(row.get("pf") == null ? "" : row.get("pf").toString(), 0d);
			for (String token : tokenizeForWordCloud(text)) {
				String w = token == null ? "" : token.trim();
				if (w.length() < 2 || w.length() > 8) {
					continue;
				}
				if (PURE_DIGITS_PATTERN.matcher(w).matches()) {
					continue;
				}
				if (stopWords.contains(w)) {
					continue;
				}
				if (isLowQualityWord(w)) {
					continue;
				}
				WordStat stat = stats.computeIfAbsent(w, k -> new WordStat());
				stat.freq++;
				stat.pfSum += pf;
			}
		}
		return stats;
	}

	private List<Map<String, Object>> toWordCloudItems(Map<String, WordStat> stats, int minFreq, int topN) {
		List<Map<String, Object>> out = new ArrayList<>();
		if (stats == null || stats.isEmpty()) {
			return out;
		}
		for (Map.Entry<String, WordStat> e : stats.entrySet()) {
			String w = e.getKey();
			WordStat s = e.getValue();
			if (w == null || s == null || s.freq < minFreq) {
				continue;
			}
			double avgPf = s.freq > 0 ? (s.pfSum / s.freq) : 0d;
			// 词云字号与「出现次数」强相关（ECharts wordCloud 用 value 映射）；评分仅作微小区分同频词
			int val = Math.max(1, s.freq * 10 + (int) Math.round(Math.min(5d, Math.max(0d, avgPf))));
			Map<String, Object> one = new LinkedHashMap<>();
			one.put("name", w);
			one.put("value", val);
			one.put("freq", s.freq);
			out.add(one);
		}
		out.sort((a, b) -> {
			int av = parseIntObj(a.get("value"), 0);
			int bv = parseIntObj(b.get("value"), 0);
			if (av != bv) {
				return Integer.compare(bv, av);
			}
			int af = parseIntObj(a.get("freq"), 0);
			int bf = parseIntObj(b.get("freq"), 0);
			return Integer.compare(bf, af);
		});
		if (out.size() > topN) {
			return new ArrayList<>(out.subList(0, topN));
		}
		return out;
	}

	private Map<String, Object> loadShopWordCloudCache(int sid, int days) {
		try {
			List<Map<String, Object>> rows = databaseService.find(
					"select sid,days,source_count,words_json,updated_at from fs_shop_wordcloud_cache where sid=" + sid + " and days=" + days
							+ " limit 1");
			if (rows != null && !rows.isEmpty()) {
				return rows.get(0);
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	private boolean isWordCloudCacheExpired(Map<String, Object> cacheRow) {
		if (cacheRow == null || cacheRow.get("updated_at") == null) {
			return true;
		}
		try {
			Object v = cacheRow.get("updated_at");
			long time;
			if (v instanceof java.util.Date) {
				time = ((java.util.Date) v).getTime();
			} else {
				String s = v.toString().trim();
				java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				time = f.parse(s).getTime();
			}
			return (System.currentTimeMillis() - time) > WORDCLOUD_CACHE_TTL_MS;
		} catch (Exception e) {
			return true;
		}
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> parseItemsJson(Object wordsJsonObj) {
		if (wordsJsonObj == null) {
			return new ArrayList<>();
		}
		try {
			Object parsed = JSON.parse(wordsJsonObj.toString());
			if (parsed instanceof List) {
				return (List<Map<String, Object>>) parsed;
			}
		} catch (Exception ignored) {
		}
		return new ArrayList<>();
	}

	private int parseIntObj(Object obj, int def) {
		if (obj == null) {
			return def;
		}
		try {
			if (obj instanceof Number) {
				return ((Number) obj).intValue();
			}
			return Integer.parseInt(obj.toString().trim());
		} catch (Exception e) {
			return def;
		}
	}

	private void upsertShopWordCloudCache(int sid, int days, int sourceCount, String wordsJson) {
		String json = wordsJson == null ? "[]" : escapeSql(wordsJson);
		try {
			databaseService.executeAction(
					"insert into fs_shop_wordcloud_cache(sid,days,source_count,words_json,updated_at) values(" + sid + "," + days + ","
							+ sourceCount + ",'" + json
							+ "',now()) on duplicate key update source_count=values(source_count), words_json=values(words_json), updated_at=now()");
		} catch (Exception ignored) {
		}
	}

	private static final class WordStat {
		int freq;
		double pfSum;
	}

	/**
	 * AI 命中候选池列表（供管理端查看）
	 */
	@RequestMapping(value = "/listSensitiveAICandidate", produces = "application/json; charset=utf-8", method = {
			RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public List<Map<String, Object>> listSensitiveAICandidate(HttpServletRequest req) {
		try {
			String pre = Common.getProperty("tableprefix");
			if (pre == null || pre.trim().isEmpty()) {
				pre = "fs_";
			}
			String limit = req.getParameter("limit");
			int l = 100;
			try {
				if (limit != null && limit.trim().length() > 0) {
					l = Math.max(1, Math.min(500, Integer.parseInt(limit.trim())));
				}
			} catch (Exception ignored) {
			}
			List<Map<String, Object>> rows = databaseService.find(
					"select * from " + pre + "sensitive_ai_candidate order by id desc limit " + l);
			SensitiveCandidateSupport.enrichRows(rows);
			return rows;
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	/**
	 * 敏感拦截日志列表（供管理端排查）
	 */
	@RequestMapping(value = "/listSensitiveHitLog", produces = "application/json; charset=utf-8", method = {
			RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public List<Map<String, Object>> listSensitiveHitLog(HttpServletRequest req) {
		try {
			String pre = Common.getProperty("tableprefix");
			if (pre == null || pre.trim().isEmpty()) {
				pre = "fs_";
			}
			String limit = req.getParameter("limit");
			int l = 100;
			try {
				if (limit != null && limit.trim().length() > 0) {
					l = Math.max(1, Math.min(500, Integer.parseInt(limit.trim())));
				}
			} catch (Exception ignored) {
			}
			return databaseService.find("select * from " + pre + "sensitive_hit_log order by id desc limit " + l);
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	/**
	 * 一键采纳 AI 候选为敏感词（可传 word 覆盖）
	 */
	@RequestMapping(value = "/adoptSensitiveCandidate", produces = "application/json; charset=utf-8", method = {
			RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> adoptSensitiveCandidate(HttpServletRequest req) {
		Map<String, Object> out = new HashMap<>();
		try {
			String pre = Common.getProperty("tableprefix");
			if (pre == null || pre.trim().isEmpty()) {
				pre = "fs_";
			}
			String id = req.getParameter("id");
			if (id == null || id.trim().isEmpty()) {
				out.put("ok", false);
				out.put("error", "缺少候选 id");
				return out;
			}
			List<Map<String, Object>> rows = databaseService.find(
					"select * from " + pre + "sensitive_ai_candidate where id=" + id.trim() + " limit 1");
			if (rows == null || rows.isEmpty()) {
				out.put("ok", false);
				out.put("error", "候选不存在");
				return out;
			}
			Map<String, Object> row = rows.get(0);
			String word = req.getParameter("word");
			if (word == null || word.trim().isEmpty()) {
				String content = row.get("content") == null ? "" : row.get("content").toString().trim();
				word = extractCandidateWord(content);
			}
			if (word == null || word.trim().isEmpty()) {
				out.put("ok", false);
				out.put("error", "未提供可采纳词条，请传 word 参数");
				return out;
			}
			word = word.trim();
			List<Map<String, Object>> exists = databaseService.find(
					"select id from " + pre + "mgc where title='" + escapeSql(word) + "' limit 1");
			if (exists == null || exists.isEmpty()) {
				databaseService.executeAction(
						"insert into " + pre + "mgc(title,ownid) values('" + escapeSql(word) + "','ai_candidate')");
			}
			databaseService.executeAction(
					"update " + pre + "sensitive_ai_candidate set state="
							+ SensitiveCandidateSupport.stateCodeForAdopt() + " where id=" + id.trim());
			sensitiveContentService.onTableMutated("mgc");
			out.put("ok", true);
			out.put("word", word);
			return out;
		} catch (Exception e) {
			out.put("ok", false);
			out.put("error", e.getMessage());
			return out;
		}
	}

	/**
	 * 忽略 AI 候选
	 */
	@RequestMapping(value = "/ignoreSensitiveCandidate", produces = "application/json; charset=utf-8", method = {
			RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> ignoreSensitiveCandidate(HttpServletRequest req) {
		Map<String, Object> out = new HashMap<>();
		try {
			String pre = Common.getProperty("tableprefix");
			if (pre == null || pre.trim().isEmpty()) {
				pre = "fs_";
			}
			String id = req.getParameter("id");
			if (id == null || id.trim().isEmpty()) {
				out.put("ok", false);
				out.put("error", "缺少候选 id");
				return out;
			}
			databaseService.executeAction(
					"update " + pre + "sensitive_ai_candidate set state="
							+ SensitiveCandidateSupport.stateCodeForIgnore() + " where id=" + id.trim());
			out.put("ok", true);
			return out;
		} catch (Exception e) {
			out.put("ok", false);
			out.put("error", e.getMessage());
			return out;
		}
	}

	private static String escapeSql(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("'", "''");
	}

	private static String extractCandidateWord(String text) {
		if (text == null) {
			return "";
		}
		String t = text.trim();
		if (t.isEmpty()) {
			return "";
		}
		Matcher m = Pattern.compile("[\\u4e00-\\u9fa5A-Za-z0-9]{2,16}").matcher(t);
		while (m.find()) {
			String s = m.group();
			if (s != null && s.length() >= 2) {
				return s;
			}
		}
		return "";
	}

}
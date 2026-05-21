package com.ideabobo.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

/**
 * 百度千帆 OpenAI 兼容 Chat Completions（与 {@code DatabaseController#aiGenBlog} 等一致）。
 * <p>
 * 鉴权：{@code Authorization: Bearer } + 控制台 API Key（bce-v3/ALTAK-…）。若 Key 在控制台被限制为「特定应用」，
 * 须在同一控制台创建 ModelBuilder 应用取得 {@code app-xxxx}，并配置 {@code qianfan_appid}，请求会自动带 {@code appid} 头；
 * 或将 API Key 的授权资源改为「所有资源」即可不传 appid。
 */
public final class QianfanChatClient {

	public static final String CHAT_COMPLETIONS_URL = "https://qianfan.baidubce.com/v2/chat/completions";
	/** 配置缺失时回退值（须为 /v2/models 列表中存在的 id，与控制台开通一致） */
	public static final String DEFAULT_QIANFAN_MODEL = "ernie-4.0-turbo-8k";

	private QianfanChatClient() {
	}

	/**
	 * 构造已带好鉴权头的 POST 请求（含可选 {@code appid}）。调用方继续 {@code .body(...).execute()}。
	 *
	 * @throws IllegalStateException 未配置 {@code qianfan_api_key}
	 */
	public static HttpRequest newChatCompletionsRequest() {
		String apiKey = Common.getProperty("qianfan_api_key");
		if (apiKey == null || apiKey.trim().isEmpty()) {
			throw new IllegalStateException("未配置千帆 API Key（application.properties 中 qianfan_api_key）");
		}
		HttpRequest req = HttpRequest.post(CHAT_COMPLETIONS_URL)
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + apiKey.trim());
		String appid = Common.getProperty("qianfan_appid");
		if (appid != null && !appid.trim().isEmpty()) {
			req.header("appid", appid.trim());
		}
		return req;
	}

	/**
	 * @param systemPrompt 可为 null 或空，则仅发送 user 消息
	 * @param userMessage  用户侧完整内容
	 * @return assistant 文本
	 */
	public static String chat(String systemPrompt, String userMessage) throws Exception {
		if (userMessage == null) {
			userMessage = "";
		}
		String model = Common.getProperty("qianfan_model");
		if (model == null || model.trim().isEmpty()) {
			model = DEFAULT_QIANFAN_MODEL;
		}
		JSONObject body = new JSONObject();
		body.set("model", model.trim());
		JSONArray messages = new JSONArray();
		if (systemPrompt != null && systemPrompt.length() > 0) {
			JSONObject sys = new JSONObject();
			sys.set("role", "system");
			sys.set("content", systemPrompt);
			messages.add(sys);
		}
		JSONObject usr = new JSONObject();
		usr.set("role", "user");
		usr.set("content", userMessage);
		messages.add(usr);
		body.set("messages", messages);

		String resp = newChatCompletionsRequest()
				.body(body.toString())
				.timeout(120000)
				.execute()
				.body();

		JSONObject respJson = new JSONObject(resp);
		if (respJson.containsKey("error")) {
			Object err = respJson.get("error");
			String hint = "";
			String es = err == null ? "" : err.toString();
			if (es.contains("invalid_appId") || es.contains("appId")) {
				hint = " 提示：控制台 API Key 若绑定「特定应用」须在配置中增加 qianfan_appid（千帆 ModelBuilder 应用 ID，形如 app-xxxxx）；或在 API Key 编辑里将授权资源改为「所有资源」。";
			} else if (es.contains("invalid_model") || es.contains("no_such_model")) {
				hint = " 提示：qianfan_model 必须与 GET https://qianfan.baidubce.com/v2/models 返回的 data[].id 完全一致（区分 ernie-speed-pro-128k 与错误的 ernie-speed-128k）。可在 application.properties 中改为 ernie-4.0-turbo-8k、ernie-3.5-8k、ernie-speed-pro-128k 等已列出且已开通的 id。";
			}
			throw new RuntimeException("千帆接口错误: " + err + hint);
		}
		String resultText = "";
		if (respJson.getJSONArray("choices") != null && respJson.getJSONArray("choices").size() > 0) {
			JSONObject choice0 = respJson.getJSONArray("choices").getJSONObject(0);
			if (choice0.getJSONObject("message") != null) {
				resultText = choice0.getJSONObject("message").getStr("content");
			}
		}
		return resultText == null ? "" : resultText.trim();
	}
}

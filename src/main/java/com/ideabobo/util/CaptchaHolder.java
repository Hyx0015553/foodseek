package com.ideabobo.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码暂存（内存），小程序无 Cookie Session 时用 captchaId 关联。
 */
public class CaptchaHolder {

	private static final long TTL_MS = 3 * 60 * 1000L;
	private static final Map<String, Entry> STORE = new ConcurrentHashMap<>();

	private static class Entry {
		final String code;
		final long expireAt;

		Entry(String code, long expireAt) {
			this.code = code;
			this.expireAt = expireAt;
		}
	}

	public static String store(String code) {
		String id = UUID.randomUUID().toString().replace("-", "");
		STORE.put(id, new Entry(code == null ? "" : code.trim().toLowerCase(), System.currentTimeMillis() + TTL_MS));
		prune();
		return id;
	}

	/**
	 * 校验成功后删除，防止重复使用。
	 */
	public static boolean verifyAndConsume(String id, String input) {
		if (id == null || id.isEmpty()) {
			return false;
		}
		Entry e = STORE.remove(id);
		if (e == null) {
			return false;
		}
		if (System.currentTimeMillis() > e.expireAt) {
			return false;
		}
		if (input == null) {
			return false;
		}
		return e.code.equals(input.trim().toLowerCase());
	}

	private static void prune() {
		if (STORE.size() < 500) {
			return;
		}
		long now = System.currentTimeMillis();
		STORE.entrySet().removeIf(en -> en.getValue().expireAt < now);
	}
}

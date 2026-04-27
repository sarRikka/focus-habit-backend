package com.atomic.focus.common.util;

import java.security.SecureRandom;

/**
 * 业务 ID 生成器：使用 "前缀_随机字符串" 形式，与 API 文档约定保持一致（如 g_xxx、p_xxx）。
 * 随机部分使用 12 位 base36 字符串，并发安全。
 */
public final class IdGenerator {

    private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int RANDOM_LEN = 12;

    private IdGenerator() {
    }

    public static String next(String prefix) {
        StringBuilder sb = new StringBuilder(prefix.length() + 1 + RANDOM_LEN);
        sb.append(prefix).append('_');
        for (int i = 0; i < RANDOM_LEN; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }

    public static String user()         { return next("u"); }
    public static String goal()         { return next("g"); }
    public static String phase()        { return next("p"); }
    public static String reward()       { return next("r"); }
    public static String checkin()      { return next("c"); }
    public static String review()       { return next("rev"); }
    public static String scene()        { return next("s"); }
    public static String notification() { return next("n"); }
    public static String task()         { return next("task"); }
    public static String session()      { return next("sess"); }
}

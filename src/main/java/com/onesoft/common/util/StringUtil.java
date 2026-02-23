package com.onesoft.common.util;

public final class StringUtil {

    private StringUtil() {}

    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}

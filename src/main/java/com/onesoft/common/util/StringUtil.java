package com.onesoft.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringUtil {
	// 클래스 단위 Logger 생성
    private static final Logger logger = LoggerFactory.getLogger(StringUtil.class);
    
    public static boolean isEmpty(String str) {
        boolean result = str == null || str.isEmpty();
        logger.debug("isEmpty called with: {}, result: {}", str, result);
        return result;
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}

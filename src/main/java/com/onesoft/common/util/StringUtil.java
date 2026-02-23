package com.onesoft.common.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;

public class StringUtil {
	private static final Logger logger = LoggerFactory.getLogger(StringUtil.class);
    /**
     * 이메일 주소를 안전하게 정리
     * - 공백 제거
     * - 괄호/인용 부호 처리
     */
    public static String sanitizeEmailAddress(String rawAddr) {
        if (rawAddr == null || rawAddr.isEmpty()) return rawAddr;

        String cleanedAddr = rawAddr.replaceAll("\\s+", " ").trim();
        cleanedAddr = cleanedAddr.replaceAll("^[\"']+|[\"']+$", "");

        int openCount = cleanedAddr.length() - cleanedAddr.replace("(", "").length();
        int closeCount = cleanedAddr.length() - cleanedAddr.replace(")", "").length();
        if (closeCount > openCount) cleanedAddr = cleanedAddr.replace(")", "");
        else if (openCount > closeCount) cleanedAddr = cleanedAddr.replace("(", "");

        cleanedAddr = cleanedAddr.replaceAll("\\(([^)]+)\\)", "<$1>");

        return cleanedAddr;
    }

    /**
     * 문자열 null/empty 여부 체크
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    /** 텍스트 클린업: 빈 줄 제거, 탭 → 공백, 길이 제한 */
    public static String cleanText(String text) {
        if (text == null) return "";
        try {
            String cleaned = Arrays.stream(text.split("\n"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.matches("[-\\s]*"))
                    .map(line -> line.replaceAll("\t+", " "))
                    .collect(Collectors.joining("\n"));
            final int MAX_LENGTH = 20_000_000;
            return cleaned.length() > MAX_LENGTH ? cleaned.substring(0, MAX_LENGTH) : cleaned;
        } catch (Exception e) {
            logger.warn("텍스트 클린업 실패", e);
            return text;
        }
    }

    /** HTML → 텍스트로 변환 후 클린업 */
    public static String cleanHtmlText(String html) {
        if (html == null) return "";
        try {
            html = html.replaceAll("(?i)<br\\s*/?>", "\n");
            String stripped = Jsoup.parse(html).text();
            return cleanText(stripped);
        } catch (Exception e) {
            logger.warn("HTML 클린업 실패", e);
            return html;
        }
    }

    /** Base64 가능성 확인 */
    public static boolean looksLikeBase64(byte[] data) {
        try {
            String s = new String(data, StandardCharsets.US_ASCII);
            return s.replaceAll("\\r|\\n", "").matches("^[A-Za-z0-9+/=]+$");
        } catch (Exception e) {
            logger.warn("Base64 검사 실패", e);
            return false;
        }
    }

    /** 문자열 → 32비트 Murmur3 해시(hex) */
    public static String getMurmurHashHex(String text) {
        try {
            int hash = Hashing.murmur3_32_fixed()
                    .hashString(text, StandardCharsets.UTF_8)
                    .asInt();
            return String.format("%08x", hash);
        } catch (Exception e) {
            logger.warn("MurmurHash 계산 실패", e);
            return "";
        }
    }
}
package com.onesoft.common.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;

/**
 * 문자열 관련 유틸리티 클래스
 */
public class StringUtil {
    private static final Logger logger = LoggerFactory.getLogger(StringUtil.class);
    private StringUtil() {}

    private static final int MAX_TEXT_LENGTH = 20_000_000;

    // ===============================
    // 문자열 존재 여부
    // ===============================

    /** 문자열이 null이거나 빈 문자열인지 확인 */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /** 문자열이 null이 아니고 빈 문자열이 아닌지 확인 */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    // ===============================
    // 이메일 처리
    // ===============================

    /**
     * 이메일 주소를 안전하게 정리
     * - 공백 제거 및 정규화
     * - 괄호/인용 부호 처리
     * @param rawAddr 원본 이메일 문자열
     * @return 정리된 이메일 문자열
     */
    public static String sanitizeEmailAddress(String rawAddr) {
        if (isEmpty(rawAddr)) return rawAddr;

        String cleaned = rawAddr.replaceAll("\\s+", " ").trim();
        cleaned = cleaned.replaceAll("^[\"']+|[\"']+$", "");

        int openCount = cleaned.length() - cleaned.replace("(", "").length();
        int closeCount = cleaned.length() - cleaned.replace(")", "").length();
        if (closeCount > openCount) cleaned = cleaned.replace(")", "");
        else if (openCount > closeCount) cleaned = cleaned.replace("(", "");

        cleaned = cleaned.replaceAll("\\(([^)]+)\\)", "<$1>");

        return cleaned;
    }

    // ===============================
    // 텍스트/HTML 처리
    // ===============================

    /**
     * 텍스트 클린업
     * - 빈 줄 제거
     * - 탭 → 공백
     * - 길이 제한
     */
    public static String cleanText(String text) {
        if (text == null) return "";

        try {
            String cleaned = Arrays.stream(text.split("\n"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.matches("[-\\s]*"))
                    .map(line -> line.replaceAll("\t+", " "))
                    .collect(Collectors.joining("\n"));
            return cleaned.length() > MAX_TEXT_LENGTH ? cleaned.substring(0, MAX_TEXT_LENGTH) : cleaned;
        } catch (Exception e) {
            logger.warn("텍스트 클린업 실패", e);
            return text;
        }
    }

    /**
     * HTML → 텍스트로 변환 후 클린업
     */
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

    // ===============================
    // Base64 및 해시
    // ===============================

    /** 바이트 배열이 Base64 가능성 있는지 확인 */
    public static boolean looksLikeBase64(byte[] data) {
        if (data == null || data.length == 0) return false;
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
        if (text == null) return "";
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

    // ===============================
    // 파일명 처리
    // ===============================

    /** 파일 확장자 추출 (예: "pdf", "docx") */
    public static String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Windows 파일명 금지 문자 제거 및 길이 제한
     * - \ / : * ? " < > | → _
     * - 예약어(CON, PRN 등) 처리
     */
    public static String sanitizeFileName(String name) {
        if (name == null) return null;

        String cleaned = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        String upper = cleaned.toUpperCase();
        if (upper.matches("CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]")) {
            cleaned = "_" + cleaned;
        }

        if (cleaned.length() > 240) cleaned = cleaned.substring(0, 240);

        return cleaned.trim().replaceAll("[\\p{Cntrl}]", "");
    }
    
    

}
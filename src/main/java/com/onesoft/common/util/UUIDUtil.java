package com.onesoft.common.util;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.onesoft.common.constant.Constants;

public class UUIDUtil {
	private static final Logger logger = LoggerFactory.getLogger(UUIDUtil.class);

    /**
     * SHA-256 기반 짧은 UUID
     */
    public static String fileUUID(String filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(filePath.getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash)
                    .substring(0, 22); // 22자
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 주어진 File 객체의 절대경로 문자열을 기반으로
     * SHA-256 해시 → Base64 URL-safe 인코딩 → 22자리 고유 ID를 생성합니다.
     *
     * <p>
     * - 파일 내용이 아닌 "경로 기반" 해시입니다.
     * - URL-safe 문자열입니다.
     * - padding("=") 제거
     * - 길이 22자로 고정
     * </p>
     *
     * @param file 대상 파일 객체
     * @return 22자리 고유 ID, 문제 발생 시 null 반환
     */
    public static String generateUniqueId(File file) {
        try {
            if (file == null) {
                logger.error("파일 객체가 null입니다.");
                return null;
            }

            // 존재하지 않아도 경로 문자열 기준으로 ID 생성 가능
            String filePath = file.getAbsolutePath();

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(filePath.getBytes(StandardCharsets.UTF_8));

            // Base64 URL-safe + padding 제거
            String encoded = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);

            // 22자리만 사용 (고유 ID 목적)
            return encoded.length() >= 22 ? encoded.substring(0, 22) : encoded;

        } catch (Exception e) {
            logger.error("고유 ID 생성 중 예외 발생: {}", file, e);
            return null;
        }
    }
    

    /**
     * 현재 파일 경로에서 가장 바깥쪽 "_unpacked" 폴더를 기준으로
     * 원본 파일의 uniqueId를 계산하여 반환합니다.
     *
     * 예:
     * /data/sample/aa.zip_unpacked/dir/test.docx
     * → /data/sample/aa.zip
     *
     * /data/sample/aa.zip_unpacked/test.zip_unpacked/test.docx
     * → /data/sample/aa.zip
     *
     * 조건에 맞지 않으면 null 반환
     *
     * @param file 대상 파일
     * @return parent uniqueId 또는 null
     */
    public static String getRootParentUniqueId(File file) {

        try {
            if (file == null) {
                logger.error("파일 객체가 null입니다.");
                return null;
            }

            String absolutePath = file.getAbsolutePath();

            int unpackedIndex = absolutePath.indexOf(Constants.UNPACKED_SUFFIX);
            if (unpackedIndex == -1) {
                // _unpacked 구조가 아님
                return null;
            }

            // "_unpacked" 끝 위치까지 포함
            int endIndex = unpackedIndex + Constants.UNPACKED_SUFFIX.length();

            // 예: /data/sample/aa.zip_unpacked
            String unpackedRootPath = absolutePath.substring(0, endIndex);

            // "_unpacked" 제거
            String originalFilePath =
                    unpackedRootPath.substring(0,
                            unpackedRootPath.length() - Constants.UNPACKED_SUFFIX.length());

            File originalFile = new File(originalFilePath);

            return generateUniqueId(originalFile);

        } catch (Exception e) {
            logger.error("root parentUniqueId 계산 중 예외 발생: {}", file, e);
            return null;
        }
    }
    
    /**
     * 현재 파일 기준으로
     * 가장 가까운 "_unpacked" 폴더에 해당하는
     * 직계 압축파일의 uniqueId를 반환합니다.
     *
     * 예:
     * /aa.zip_unpacked/test.zip_unpacked/doc.txt
     * → test.zip 의 uniqueId
     *
     * 조건에 맞지 않으면 null 반환
     */
    public static String getParentUniqueId(File file) {

        try {
            if (file == null) {
                logger.error("파일 객체가 null입니다.");
                return null;
            }

            String absolutePath = file.getAbsolutePath();

            int unpackedIndex = absolutePath.lastIndexOf(Constants.UNPACKED_SUFFIX);
            if (unpackedIndex == -1) {
                return null;
            }

            int suffixLength = Constants.UNPACKED_SUFFIX.length();
            int endIndex = unpackedIndex + suffixLength;

            // 예: /aa.zip_unpacked/test.zip_unpacked
            String unpackedPath = absolutePath.substring(0, endIndex);

            // "_unpacked" 제거 → test.zip
            String originalFilePath =
                    unpackedPath.substring(0, unpackedPath.length() - suffixLength);

            File originalFile = new File(originalFilePath);

            return generateUniqueId(originalFile);

        } catch (Exception e) {
            logger.error("직계 parentUniqueId 계산 중 예외 발생: {}", file, e);
            return null;
        }
    }

    /**
     * 랜덤 UUID를 22자로 압축
     */
    public static String shortUUID() {
        UUID uuid = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bb.array());
    }
}
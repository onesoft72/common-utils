package com.onesoft.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 파일 관련 유틸리티 클래스
 */
public class FileUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    // 상수 선언
    private static final byte[] OLE2_HEADER = {(byte)0xD0,(byte)0xCF,0x11,(byte)0xE0,(byte)0xA1,(byte)0xB1,0x1A,(byte)0xE1};
    private static final byte[] PDF_HEADER = {0x25,0x50,0x44,0x46,0x2D};
    private static final byte[] ZIP_HEADER = {0x50,0x4B,0x03,0x04};

    private FileUtil() {} // 인스턴스화 방지
    
    /**
     * 주어진 파일 객체에서 폴더 경로와 확장자를 제외한 순수 파일명만 반환합니다.
     * 문제 발생 시 로그를 남기고 빈 문자열("")을 반환합니다.
     *
     * @param file 대상 파일 객체
     * @return 순수 파일명(확장자 제외), 문제 발생 시 빈 문자열("")
     */
    public static String getBaseFileName(File file) {
        try {
            if (file == null) {
                logger.error("파일 객체가 null입니다.");
                return "";
            }

            if (!file.exists()) {
                logger.error("파일이 존재하지 않습니다: {}", file.getAbsolutePath());
                return "";
            }

            if (file.isDirectory()) {
                logger.error("디렉토리는 파일명으로 변환할 수 없습니다: {}", file.getAbsolutePath());
                return "";
            }

            String fileName = file.getName();

            if (fileName.isEmpty()) {
                logger.error("파일명이 비어 있습니다: {}", file.getAbsolutePath());
                return "";
            }

            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                return fileName.substring(0, dotIndex);
            } else {
                // 확장자가 없는 파일
                return fileName;
            }

        } catch (Exception e) {
            // 예외 발생 시 로깅 후 빈 문자열 반환
            logger.error("파일명 추출 중 예외 발생: {}", file, e);
            return "";
        }
    }
    
 // Windows에서 허용되지 않는 파일명 문자
    private static final String INVALID_CHARS_REGEX = "[\\\\/:*?\"<>|]";
    private static final int MAX_FOLDER_NAME_LENGTH = 255; // OS 제한 고려

    /**
     * 주어진 파일 객체를 기준으로, 파일명+'_unpacked' 하위 폴더를 안전하게 생성하고
     * 그 File 객체를 반환합니다.
     * 문제가 발생하면 로그를 남기고 null을 반환합니다.
     *
     * @param file 대상 파일 객체
     * @return 생성된 "_unpacked" 하위 폴더 File 객체, 문제가 있으면 null
     */
    public static File getUnpackedFolder(File file) {
        try {
            if (file == null) {
                logger.error("파일 객체가 null입니다.");
                return null;
            }

            if (!file.exists()) {
                logger.error("파일이 존재하지 않습니다: {}", file.getAbsolutePath());
                return null;
            }

            if (file.isDirectory()) {
                logger.error("디렉토리는 대상 파일이 될 수 없습니다: {}", file.getAbsolutePath());
                return null;
            }

            // 파일명 그대로 사용, 특수문자는 "_"로 대체
            String safeFileName = file.getName().replaceAll(INVALID_CHARS_REGEX, "_");
            String unpackedFolderName = safeFileName + "_unpacked";

            // 너무 길면 잘라서 처리
            if (unpackedFolderName.length() > MAX_FOLDER_NAME_LENGTH) {
                unpackedFolderName = unpackedFolderName.substring(0, MAX_FOLDER_NAME_LENGTH);
            }

            // 원본 파일이 있는 폴더에 하위 폴더 생성
            File parentDir = file.getParentFile();
            if (parentDir == null) {
                logger.error("파일의 부모 디렉토리를 찾을 수 없습니다: {}", file.getAbsolutePath());
                return null;
            }

            File unpackedFolder = new File(parentDir, unpackedFolderName);

            if (!unpackedFolder.exists()) {
                boolean created = unpackedFolder.mkdirs();
                if (!created) {
                    logger.error("폴더 생성 실패: {}", unpackedFolder.getAbsolutePath());
                    return null;
                }
            }

            return unpackedFolder;

        } catch (Exception e) {
            logger.error("Unpacked 폴더 생성 중 예외 발생: {}", file, e);
            return null;
        }
    }

    // ===============================
    // 파일 형식 관련
    // ===============================

    /**
     * OLE2 파일(xls, doc 등) 여부 확인
     */
    public static boolean isOLE2File(File file) {
        if (file == null || !file.exists() || !file.isFile()) return false;

        try (InputStream is = new FileInputStream(file)) {
            byte[] header = new byte[OLE2_HEADER.length];
            if (is.read(header) != OLE2_HEADER.length) return false;
            boolean result = Arrays.equals(header, OLE2_HEADER);
            logger.debug("OLE2 검사: {} → {}", file.getName(), result);
            return result;
        } catch (IOException e) {
            logger.warn("OLE2 파일 검사 실패: {}", file.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * PDF 파일 여부 확인
     */
    public static boolean isPdfFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) return false;

        try (InputStream is = new FileInputStream(file)) {
            byte[] header = new byte[PDF_HEADER.length];
            if (is.read(header) != PDF_HEADER.length) return false;
            boolean result = Arrays.equals(header, PDF_HEADER);
            logger.debug("PDF 검사: {} → {}", file.getName(), result);
            return result;
        } catch (IOException e) {
            logger.warn("PDF 파일 검사 실패: {}", file.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * ZIP 파일 여부 확인 (매직 넘버 기준)
     */
    public static boolean isZipFile(File file) {
        if (file == null || !file.exists() || file.length() < ZIP_HEADER.length) return false;

        try (InputStream is = new FileInputStream(file)) {
            byte[] header = new byte[ZIP_HEADER.length];
            if (is.read(header) != ZIP_HEADER.length) return false;
            boolean result = Arrays.equals(header, ZIP_HEADER);
            logger.debug("ZIP 검사: {} → {}", file.getName(), result);
            return result;
        } catch (IOException e) {
            logger.warn("ZIP 파일 검사 실패: {}", file.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * Office 파일(.xls, .xlsx, .doc, .docx 등) 암호화 여부 확인
     */
    public static boolean isOfficeFileEncrypted(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            logger.warn("유효하지 않은 파일: {}", file);
            return false;
        }

        try (InputStream is = new FileInputStream(file)) {
            if (isOLE2File(file)) {
                // OLE2 파일 처리
                try (POIFSFileSystem fs = new POIFSFileSystem(is)) {
                    EncryptionInfo info = new EncryptionInfo(fs);
                    Decryptor decryptor = Decryptor.getInstance(info);
                    boolean canOpen = decryptor.verifyPassword(Decryptor.DEFAULT_PASSWORD);
                    logger.debug("OLE2 암호화 여부: {} → {}", file.getName(), !canOpen);
                    return !canOpen;
                } catch (EncryptedDocumentException ede) {
                    logger.debug("OLE2 암호화 파일: {}", file.getName());
                    return true;
                }
            } else {
                // OOXML 파일 처리
                try (OPCPackage opc = OPCPackage.open(file)) {
                    logger.debug("OOXML 정상 열림: {}", file.getName());
                    return false;
                } catch (EncryptedDocumentException ede) {
                    logger.debug("OOXML 암호화 파일: {}", file.getName());
                    return true;
                }
            }
        } catch (Exception e) {
            logger.warn("Office 파일 암호화 확인 실패: {}", file.getName(), e);
            return false;
        }
    }

    // ===============================
    // 폴더 및 파일 크기
    // ===============================

    /** 폴더 크기 계산 (재귀) */
    public static long getFolderSize(File folder) {
        if (folder == null || !folder.isDirectory()) return 0;

        long size = 0;
        File[] files = folder.listFiles();
        if (files == null) return 0;

        for (File f : files) {
            size += f.isFile() ? f.length() : getFolderSize(f);
        }
        logger.debug("폴더 크기 계산: {} → {} bytes", folder.getAbsolutePath(), size);
        return size;
    }

    /** ZIP 파일에 빈(entry 이름 없는) 항목 존재 여부 확인 */
    public static boolean hasEmptyZipEntry(File file) {
        if (!isZipFile(file)) return false;

        try (ZipFile zip = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName() == null || entry.getName().trim().isEmpty()) {
                    logger.debug("ZIP 빈 entry 발견: {}", file.getName());
                    return true;
                }
            }
        } catch (IOException e) {
            logger.warn("ZIP 파일 검사 실패: {}", file.getAbsolutePath(), e);
            return false;
        }
        return false;
    }

    // ===============================
    // 파일명/경로 처리
    // ===============================

    /** 동일 파일명 존재 시 (1), (2), ... 형식으로 변경 */
    public static File resolveDuplicateFile(File dir, String originalFileName) {
        File file = new File(dir, originalFileName);
        if (!file.exists()) return file;

        String base = originalFileName;
        String ext = "";
        int dot = originalFileName.lastIndexOf('.');
        if (dot != -1) { base = originalFileName.substring(0, dot); ext = originalFileName.substring(dot); }

        int count = 1;
        while (file.exists()) {
            file = new File(dir, base + "(" + count + ")" + ext);
            count++;
        }
        logger.debug("중복 파일명 처리: {} → {}", originalFileName, file.getName());
        return file;
    }

}
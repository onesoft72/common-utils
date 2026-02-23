package com.onesoft.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
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

public class FileUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
    
    private static final byte[] OLE2_HEADER = {(byte)0xD0,(byte)0xCF,0x11,(byte)0xE0,(byte)0xA1,(byte)0xB1,0x1A,(byte)0xE1};
    private static final byte[] PDF_HEADER = {0x25,0x50,0x44,0x46,0x2D};

    /** 파일이 OLE2 형식(xls, doc 등)인지 확인 */
    public static boolean isOLE2File(File file) {
        try (InputStream is = new FileInputStream(file)) {
            byte[] header = new byte[OLE2_HEADER.length];
            if (is.read(header) != OLE2_HEADER.length) return false;
            return Arrays.equals(header, OLE2_HEADER);
        } catch (IOException e) {
            logger.warn("OLE2 파일 검사 실패: {}", file.getName(), e);
            return false;
        }
    }

    /** ZIP 파일 내 빈(entry 이름이 없는) 항목 존재 여부 확인 */
    public static boolean hasEmptyZipEntry(File file) {
        try (ZipFile zipFile = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName() == null || entry.getName().trim().isEmpty()) {
                    return true;
                }
            }
        } catch (IOException e) {
            logger.warn("ZIP 파일 검사 실패: {}", file.getName(), e);
            return false;
        }
        return false;
    }

    /** 폴더 크기 계산 */
    public static long getFolderSize(File folder) {
        if (!folder.isDirectory()) return 0;
        long size = 0;
        File[] files = folder.listFiles();
        if (files == null) return 0;
        for (File f : files) {
            size += f.isFile() ? f.length() : getFolderSize(f);
        }
        return size;
    }

    /** PDF 파일 여부 확인 */
    public static boolean isPdfFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) return false;
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[PDF_HEADER.length];
            if (fis.read(header) < PDF_HEADER.length) return false;
            return Arrays.equals(header, PDF_HEADER);
        } catch (IOException e) {
            logger.warn("PDF 파일 검사 실패: {}", file.getName(), e);
            return false;
        }
    }

    /**
     * Office 파일(.xls, .xlsx, .doc, .docx 등)의 암호화 여부를 확인합니다.
     * - OLE2 파일(.xls, .doc 등) : POIFSFileSystem + EncryptionInfo 사용
     * - OOXML 파일(.xlsx, .docx 등) : OPCPackage로 열기 시도
     *
     * @param file 확인할 Office 파일
     * @return 암호화되어 있으면 true, 아니면 false
     */
    public static boolean isOfficeFileEncrypted(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            logger.warn("유효하지 않은 파일: {}", file);
            return false;
        }

        try (InputStream is = new FileInputStream(file)) {
            if (isOLE2File(file)) {
                // OLE2 포맷 (xls, doc, ppt 등)
                try (POIFSFileSystem fs = new POIFSFileSystem(is)) {
                    EncryptionInfo info = new EncryptionInfo(fs);
                    Decryptor decryptor = Decryptor.getInstance(info);

                    boolean canOpen = decryptor.verifyPassword(Decryptor.DEFAULT_PASSWORD);
                    if (!canOpen) {
                        logger.debug("OLE2 파일 암호화 확인: {}", file.getName());
                    }
                    return !canOpen;
                } catch (EncryptedDocumentException ede) {
                    logger.debug("OLE2 암호화 파일: {}", file.getName());
                    return true;
                }
            } else {
                // OOXML 포맷 (xlsx, docx, pptx 등)
                try (OPCPackage opc = OPCPackage.open(file)) {
                    logger.debug("OOXML 파일 정상 열림: {}", file.getName());
                    return false; // 정상 열림 → 암호화 아님
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

    /** 동일 파일명 존재 시 (1), (2) ... 방식으로 변경 */
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
        return file;
    }

    /**
     * 파일 확장자를 반환합니다. (예: "pdf", "docx")
     */
    public static String getFileExtension(File file) {
        if (file == null) return "";
        String name = file.getName();
        int lastDot = name.lastIndexOf(".");
        if (lastDot > 0 && lastDot < name.length() - 1) {
            return name.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    /**
     * ZIP 파일 여부 확인 (헤더 체크)
     */
    public static boolean isZipFile(File file) {
        if (file == null || !file.exists() || file.length() < 4) return false;

        try (InputStream is = new FileInputStream(file)) {
            byte[] signature = new byte[4];
            if (is.read(signature) != 4) return false;

            return (signature[0] == 0x50 && signature[1] == 0x4B &&
                   (signature[2] == 0x03 || signature[2] == 0x05 || signature[2] == 0x07) &&
                   (signature[3] == 0x04 || signature[3] == 0x06 || signature[3] == 0x08));
        } catch (IOException e) {
            logger.error("ZIP 파일 확인 실패: {}", file.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * 파일명에서 Windows 금지 문자 제거 및 길이 제한
     */
    public static String sanitizeFileName(String name) {
        if (name == null) return null;

        // 1. 금지 문자 제거
        String cleaned = name.replaceAll("[\\\\/:*?\"<>|]", "_");

        // 2. 예약어 방지 (윈도우)
        String upper = cleaned.toUpperCase();
        if (upper.matches("CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]")) {
            cleaned = "_" + cleaned;
        }

        // 3. 길이 제한
        if (cleaned.length() > 240) cleaned = cleaned.substring(0, 240);

        // 4. 공백/제어문자 제거
        cleaned = cleaned.trim().replaceAll("[\\p{Cntrl}]", "");

        return cleaned;
    }

    /**
     * 절대 경로를 안전하게 sanitize
     */
    public static Path sanitizePath(Path originalPath) {
        if (originalPath == null) return null;

        Path sanitized = originalPath.getRoot();
        for (Path part : originalPath) {
            sanitized = sanitized == null ?
                        Path.of(sanitizeFileName(part.toString())) :
                        sanitized.resolve(sanitizeFileName(part.toString()));
        }
        return sanitized;
    }
}
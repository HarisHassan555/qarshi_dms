package com.bezkoder.spring.login.admin.utility.common;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.UUID;

/**
 * Stores official archive documents under {@code ~/.vim_dms_uploads/official-documents/}.
 */
public final class OfficialDocumentStorageUtil {

    public static final String DIR = "official-documents";
    private static final long MAX_BYTES = 50L * 1024L * 1024L;

    private OfficialDocumentStorageUtil() {
    }

    public static String save(int documentId, MultipartFile file) throws IOException {
        if (documentId <= 0 || file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Document file is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("File must be 50 MB or smaller");
        }
        String extension = extensionFrom(file);
        String relativePath = DIR + "/doc-" + documentId + "-" + UUID.randomUUID() + "." + extension;
        File target = new File(SignatureStorageUtil.uploadRoot(), relativePath.replace('/', File.separatorChar));
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create document folder");
        }
        Files.write(target.toPath(), file.getBytes());
        return relativePath;
    }

    public static File resolve(String relativePath) {
        if (relativePath == null) {
            return null;
        }
        String normalized = relativePath.trim().replace('\\', '/');
        if (normalized.isEmpty() || normalized.contains("..") || !normalized.startsWith(DIR + "/")) {
            return null;
        }
        File file = new File(SignatureStorageUtil.uploadRoot(), normalized.replace('/', File.separatorChar));
        if (!file.isFile() || !file.canRead()) {
            return null;
        }
        return file;
    }

    private static String extensionFrom(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            String ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT).trim();
            if (!ext.isEmpty() && ext.length() <= 10) {
                return sanitizeExt(ext);
            }
        }
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";
        if (contentType.contains("pdf")) {
            return "pdf";
        }
        if (contentType.contains("png")) {
            return "png";
        }
        if (contentType.contains("jpeg") || contentType.contains("jpg")) {
            return "jpg";
        }
        if (contentType.contains("webp")) {
            return "webp";
        }
        return "bin";
    }

    private static String sanitizeExt(String ext) {
        if (ext.matches("[a-z0-9]+")) {
            return ext;
        }
        return "bin";
    }
}

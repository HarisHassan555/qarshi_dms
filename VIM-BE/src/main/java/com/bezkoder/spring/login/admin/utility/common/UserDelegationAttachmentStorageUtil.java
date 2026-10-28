package com.bezkoder.spring.login.admin.utility.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;

public final class UserDelegationAttachmentStorageUtil {

    public static final String DELEGATIONS_DIR = "user-delegations";
    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final Pattern SAFE_EXT = Pattern.compile("^[a-z0-9]{1,10}$");

    private UserDelegationAttachmentStorageUtil() {
    }

    public static File resolveAttachmentFile(String attachmentPath) {
        if (attachmentPath == null || attachmentPath.trim().isEmpty()) {
            return null;
        }
        String trimmed = attachmentPath.trim().replace('/', File.separatorChar).replace('\\', File.separatorChar);
        File file = new File(trimmed);
        if (!file.isAbsolute()) {
            file = new File(SignatureStorageUtil.uploadRoot() + File.separator + trimmed);
        }
        return file.exists() && file.isFile() && file.length() > 0 ? file : null;
    }

    public static String saveDelegationAttachment(int delegationId, String fileBase64, String originalFileName)
            throws IOException {
        if (delegationId <= 0 || fileBase64 == null || fileBase64.trim().isEmpty()) {
            return null;
        }
        String mimeType = null;
        String cleaned = fileBase64.trim();
        if (cleaned.startsWith("data:")) {
            int comma = cleaned.indexOf(',');
            if (comma > 0) {
                String header = cleaned.substring(0, comma).toLowerCase(Locale.ROOT);
                int semi = header.indexOf(';');
                if (semi > 5) {
                    mimeType = header.substring(5, semi);
                }
                cleaned = cleaned.substring(comma + 1);
            }
        }
        cleaned = cleaned.replaceAll("\\s+", "");
        byte[] bytes = Base64.getDecoder().decode(cleaned);
        if (bytes.length == 0) {
            return null;
        }
        if (bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("Attachment must be 10 MB or smaller");
        }
        String ext = extensionFromFileName(originalFileName);
        if (ext == null) {
            ext = extensionFromMime(mimeType);
        }
        if (ext == null) {
            ext = extensionFromBytes(bytes);
        }
        if (ext == null) {
            throw new IllegalArgumentException("Unsupported attachment type. Use PDF, image, or Word document.");
        }
        String relativePath = DELEGATIONS_DIR + "/delegation-" + delegationId + "." + ext;
        File target = new File(SignatureStorageUtil.uploadRoot() + File.separator
                + relativePath.replace('/', File.separatorChar));
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        Files.write(target.toPath(), bytes);
        return relativePath;
    }

    public static void deleteAttachmentFile(String attachmentPath) {
        File file = resolveAttachmentFile(attachmentPath);
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    public static String sanitizeDisplayFileName(String originalFileName) {
        if (originalFileName == null) {
            return null;
        }
        String name = originalFileName.trim().replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0 && slash < name.length() - 1) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[^a-zA-Z0-9._\\- ()]", "_");
        if (name.length() > 255) {
            name = name.substring(0, 255);
        }
        return name.isEmpty() ? null : name;
    }

    private static String extensionFromFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            return null;
        }
        String name = originalFileName.trim();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot >= name.length() - 1) {
            return null;
        }
        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!SAFE_EXT.matcher(ext).matches()) {
            return null;
        }
        return ext;
    }

    private static String extensionFromMime(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        switch (mimeType.toLowerCase(Locale.ROOT)) {
            case "application/pdf":
                return "pdf";
            case "image/jpeg":
            case "image/jpg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "image/webp":
                return "webp";
            case "application/msword":
                return "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return "docx";
            default:
                return null;
        }
    }

    private static String extensionFromBytes(byte[] bytes) {
        if (bytes.length >= 4 && bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46) {
            return "pdf";
        }
        if (bytes.length >= 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) {
            return "jpg";
        }
        if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50) {
            return "png";
        }
        if (bytes.length >= 4 && bytes[0] == 0x50 && bytes[1] == 0x4B) {
            return "docx";
        }
        return null;
    }
}

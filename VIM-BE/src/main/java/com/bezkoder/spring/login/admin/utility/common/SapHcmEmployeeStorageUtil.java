package com.bezkoder.spring.login.admin.utility.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public final class SapHcmEmployeeStorageUtil {

    public static final String PHOTOS_DIR = "sap-hcm-photos";

    private SapHcmEmployeeStorageUtil() {
    }

    public static String uploadRoot() {
        return SignatureStorageUtil.uploadRoot();
    }

    public static File photosDirectory() throws IOException {
        File dir = new File(uploadRoot() + File.separator + PHOTOS_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create SAP HCM photos directory: " + dir.getAbsolutePath());
        }
        return dir;
    }

    public static String relativePhotoPath(String employeeId, String extension) {
        String safeId = sanitizeEmployeeId(employeeId);
        String ext = extension == null || extension.isEmpty() ? "jpg" : extension.replace(".", "");
        return PHOTOS_DIR + "/" + safeId + "." + ext;
    }

    public static File resolvePhotoFile(String picturePath) {
        if (picturePath == null || picturePath.trim().isEmpty()) {
            return null;
        }
        String trimmed = picturePath.trim().replace('/', File.separatorChar).replace('\\', File.separatorChar);
        File file = new File(trimmed);
        if (!file.isAbsolute()) {
            file = new File(uploadRoot() + File.separator + trimmed);
        }
        return file.exists() && file.isFile() && file.length() > 0 ? file : null;
    }

    /**
     * Decodes base64 image bytes and writes under {@link #PHOTOS_DIR}. Returns relative path for DB.
     */
    public static String saveEmployeePhoto(String employeeId, String pictureBase64) throws IOException {
        if (pictureBase64 == null || pictureBase64.trim().isEmpty()) {
            return null;
        }
        String cleaned = pictureBase64.replaceAll("\\s+", "");
        byte[] bytes = Base64.getDecoder().decode(cleaned);
        if (bytes.length == 0) {
            return null;
        }
        String ext = detectImageExtension(bytes);
        String relativePath = relativePhotoPath(employeeId, ext);
        File target = new File(uploadRoot() + File.separator + relativePath.replace('/', File.separatorChar));
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        Files.write(target.toPath(), bytes);
        return relativePath;
    }

    private static String detectImageExtension(byte[] bytes) {
        if (bytes.length >= 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) {
            return "jpg";
        }
        if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50) {
            return "png";
        }
        return "jpg";
    }

    private static String sanitizeEmployeeId(String employeeId) {
        String id = employeeId == null ? "" : employeeId.trim();
        return id.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}

package com.bezkoder.spring.login.admin.utility.common;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

/**
 * Stores application logo and dashboard hero images as files.
 * The database keeps the relative path only.
 */
public final class ApplicationSettingImageStorageUtil {

    public static final String DIR = "app-settings";
    private static final long MAX_BYTES = 8L * 1024L * 1024L;

    private ApplicationSettingImageStorageUtil() {
    }

    public static String save(int settingId, String kind, MultipartFile file) throws IOException {
        if (settingId <= 0 || file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Image must be 8 MB or smaller");
        }
        byte[] bytes = file.getBytes();
        String extension = imageExtension(bytes);
        if (extension == null) {
            throw new IllegalArgumentException("Upload a PNG, JPG, WEBP, or GIF image");
        }
        String relativePath = DIR + "/setting-" + settingId + "-" + kind + "." + extension;
        File target = new File(SignatureStorageUtil.uploadRoot(), relativePath.replace('/', File.separatorChar));
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create image folder");
        }
        Files.write(target.toPath(), bytes);
        return relativePath;
    }

    public static File resolve(String relativePath) {
        if (relativePath == null) {
            return null;
        }
        String normalized = relativePath.trim().replace('\\', '/');
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.contains("..") || lower.startsWith("data:")) {
            return null;
        }
        if (!normalized.startsWith(DIR + "/")) {
            return null;
        }
        File root = new File(SignatureStorageUtil.uploadRoot(), DIR);
        File file = new File(SignatureStorageUtil.uploadRoot(), normalized.replace('/', File.separatorChar));
        try {
            String rootPath = root.getCanonicalPath();
            String filePath = file.getCanonicalPath();
            if (!filePath.equals(rootPath) && !filePath.startsWith(rootPath + File.separator)) {
                return null;
            }
        } catch (IOException ex) {
            return null;
        }
        return file.exists() && file.isFile() && file.length() > 0 ? file : null;
    }

    public static void deleteQuietly(String relativePath) {
        File file = resolve(relativePath);
        if (file != null) {
            file.delete();
        }
    }

    public static String contentTypeFor(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".gif")) {
            return "image/gif";
        }
        if (name.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private static String imageExtension(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return "jpg";
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47) {
            return "png";
        }
        if (bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return "gif";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        return null;
    }
}

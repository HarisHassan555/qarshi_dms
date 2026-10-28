package com.bezkoder.spring.login.admin.utility.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public final class UserProfilePictureStorageUtil {

    public static final String PROFILES_DIR = "user-profiles";

    private UserProfilePictureStorageUtil() {
    }

    public static File resolveProfilePictureFile(String picturePath) {
        if (picturePath == null || picturePath.trim().isEmpty()) {
            return null;
        }
        String trimmed = picturePath.trim().replace('/', File.separatorChar).replace('\\', File.separatorChar);
        File file = new File(trimmed);
        if (!file.isAbsolute()) {
            file = new File(SignatureStorageUtil.uploadRoot() + File.separator + trimmed);
        }
        return file.exists() && file.isFile() && file.length() > 0 ? file : null;
    }

    public static String saveProfilePicture(int userId, String pictureBase64) throws IOException {
        if (userId <= 0 || pictureBase64 == null || pictureBase64.trim().isEmpty()) {
            return null;
        }
        String cleaned = pictureBase64.trim();
        if (cleaned.contains(",")) {
            cleaned = cleaned.substring(cleaned.indexOf(',') + 1);
        }
        cleaned = cleaned.replaceAll("\\s+", "");
        byte[] bytes = Base64.getDecoder().decode(cleaned);
        if (bytes.length == 0) {
            return null;
        }
        String ext = detectImageExtension(bytes);
        String relativePath = PROFILES_DIR + "/user-" + userId + "." + ext;
        File target = new File(SignatureStorageUtil.uploadRoot() + File.separator + relativePath.replace('/', File.separatorChar));
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
}

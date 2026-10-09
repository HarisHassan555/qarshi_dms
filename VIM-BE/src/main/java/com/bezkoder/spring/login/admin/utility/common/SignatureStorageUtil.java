package com.bezkoder.spring.login.admin.utility.common;

import java.io.File;

/**
 * Resolves signature image files under the shared upload root ({@code ~/.vim_dms_uploads}).
 */
public final class SignatureStorageUtil {

    private static final String SIGNATURES_DIR = "signatures";

    private SignatureStorageUtil() {
    }

    public static String uploadRoot() {
        return System.getProperty("user.home") + File.separator + ".vim_dms_uploads";
    }

    public static File resolveSignatureFile(String signaturePath) {
        if (signaturePath == null || signaturePath.trim().isEmpty()) {
            return null;
        }
        String trimmed = signaturePath.trim().replace('/', File.separatorChar).replace('\\', File.separatorChar);
        String rootPath = uploadRoot();

        File direct = new File(trimmed);
        if (!direct.isAbsolute()) {
            direct = new File(rootPath + File.separator + trimmed);
        }
        if (isReadableSignatureFile(direct)) {
            return direct;
        }

        String fileName = trimmed;
        int lastSep = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'));
        if (lastSep >= 0 && lastSep < trimmed.length() - 1) {
            fileName = trimmed.substring(lastSep + 1);
        }

        File inSignatures = new File(rootPath + File.separator + SIGNATURES_DIR + File.separator + fileName);
        if (isReadableSignatureFile(inSignatures)) {
            return inSignatures;
        }

        File inSignaturesFull = new File(rootPath + File.separator + SIGNATURES_DIR + File.separator + trimmed);
        if (isReadableSignatureFile(inSignaturesFull)) {
            return inSignaturesFull;
        }

        return null;
    }

    public static boolean hasStoredSignatureFile(String signaturePath) {
        File file = resolveSignatureFile(signaturePath);
        return isReadableSignatureFile(file);
    }

    private static boolean isReadableSignatureFile(File file) {
        return file != null && file.exists() && file.isFile() && file.length() > 0;
    }
}

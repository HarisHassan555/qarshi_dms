package com.bezkoder.spring.login.sa.bll.servicesimpl;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Minimal MultipartFile wrapper for re-analysis of files already on disk.
 */
public class MultipartFileAdapter implements MultipartFile {

    private final byte[] bytes;
    private final String originalFilename;
    private final String contentType;

    private MultipartFileAdapter(byte[] bytes, String originalFilename, String contentType) {
        this.bytes = bytes;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
    }

    public static MultipartFileAdapter fromFile(File file, String originalFilename, String contentType) throws IOException {
        return new MultipartFileAdapter(Files.readAllBytes(file.toPath()), originalFilename, contentType);
    }

    @Override
    public String getName() {
        return "file";
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return bytes.length == 0;
    }

    @Override
    public long getSize() {
        return bytes.length;
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public void transferTo(File dest) throws IOException {
        Files.write(dest.toPath(), bytes);
    }
}

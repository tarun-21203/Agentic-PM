package com.agentic.pm.service;

import com.agentic.pm.exception.InvalidFileException;

import java.util.Locale;
import java.util.Set;

public class FileValidationService {

    private final long maxFileSizeBytes;
    private final Set<String> allowedExtensions;

    public FileValidationService(long maxFileSizeBytes, Set<String> allowedExtensions) {
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.allowedExtensions = allowedExtensions;
    }

    public void validateFilename(String filename) {
        String ext = getExtension(filename);
        if (ext == null || !allowedExtensions.contains(ext.toLowerCase(Locale.ROOT))) {
            throw new InvalidFileException("File type not allowed. Allowed: " + allowedExtensions);
        }
    }

    public void validateSize(long sizeBytes) {
        if (sizeBytes > maxFileSizeBytes) {
            throw new InvalidFileException("File size exceeds maximum allowed: " + (maxFileSizeBytes / 1024 / 1024) + " MB");
        }
    }

    private static String getExtension(String filename) {
        if (filename == null || filename.isBlank()) return null;
        int i = filename.lastIndexOf('.');
        if (i < 0 || i >= filename.length() - 1) return null;
        return filename.substring(i + 1);
    }
}


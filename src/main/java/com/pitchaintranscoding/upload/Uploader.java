package com.pitchaintranscoding.upload;

import com.pitchaintranscoding.dto.UploadType;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public interface Uploader {
    String generateFileObjectKey(UploadType type, Long dirIdentifier, String fileName);

    String uploadFileSync(UploadType type, Long dirIdentifier, File file) throws IOException;

    Path createTempFile(MultipartFile file);
}
package com.pitchaintranscoding.upload;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
public abstract class FileUploader implements Uploader {

    private static final String TEMP_FILE_PATH_PREFIX = "upload-async-";
    private static final String TEMP_FILE_PATH_SUFFIX_PREFIX = "-";

    @Override
    public Path createTempFile(MultipartFile file) {
        Path tempFilePath = null;
        try {
            tempFilePath = Files.createTempFile(TEMP_FILE_PATH_PREFIX, TEMP_FILE_PATH_SUFFIX_PREFIX + file.getOriginalFilename());
            Files.copy(file.getInputStream(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Saving file succeeded");
        } catch (IOException e) {
            if (tempFilePath != null) {
                deleteTemporaryFile(tempFilePath);
            }
            throw new RuntimeException("파일 업로드에 실패했습니다.");
        }
        return tempFilePath;
    }

    public void deleteTemporaryFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("임시 파일 삭제 실패: {}", path, e);
        }
    }
}

package com.pitchaintranscoding.service;

import com.pitchaintranscoding.upload.Uploader;
import com.pitchaintranscoding.util.AsyncTranscodingExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpUploadService {
    private final Uploader uploader;
    private final AsyncTranscodingExecutor asyncTranscodingExecutor;
    private final TranscodingService transcodingService;

    public void upload(Long spId, MultipartFile file) {
        validateContentType(file);

        String fileNameFormat = UUID.randomUUID().toString();

        // MultipartFile은 HTTP Request 요청 스코프가 끝나면 사라지기 때문에 비동지 작업에서 생성 불가능
        Path inputTempFilePath = uploader.createTempFile(file);

        Runnable job = transcodingService.createJob(spId, inputTempFilePath, fileNameFormat);
        asyncTranscodingExecutor.execute(spId, job);
    }

    private static void validateContentType(MultipartFile file) {
        if (!file.getContentType().startsWith("video")) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. 비디오 파일만 업로드할 수 있습니다.");
        }
    }
}

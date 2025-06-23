package com.pitchaintranscoding.service;

import com.pitchaintranscoding.redis.RedisPublisher;
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
    private final RedisPublisher redisPublisher;

    public void upload(Long spId, MultipartFile file) {
        String fileNameFormat = UUID.randomUUID().toString();
        Path inputTempFilePath = null;
        try {
            validateContentType(file);

            // MultipartFile은 HTTP Request 요청 스코프가 끝나면 사라지기 때문에 비동지 작업에서 생성 불가능
            inputTempFilePath = uploader.createTempFile(file);
        } catch (Exception e) {
            log.info("Transcoding failed because {}", e.getMessage());
            redisPublisher.publishTranscodingFailEvent(spId);
        }

        Runnable job = transcodingService.createJob(spId, inputTempFilePath, fileNameFormat);
        asyncTranscodingExecutor.execute(spId, job);
    }


    private static void validateContentType(MultipartFile file) {
        if (!file.getContentType().startsWith("audio/mp4")) {
            log.info("Not support this contentType");
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. 비디오 파일만 업로드할 수 있습니다.");
        }
    }
}

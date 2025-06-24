package com.pitchaintranscoding.service;

import com.pitchaintranscoding.redis.RedisPublisher;
import com.pitchaintranscoding.upload.S3FileUploader;
import com.pitchaintranscoding.util.DirectoryManager;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@RequiredArgsConstructor
@Service
public class JobService {
    private final S3FileUploader s3FileUploader;
    private final TranscodingService transcodingService;
    private final RedisPublisher redisPublisher;
    private final DirectoryManager directoryManager;

    //output 경로 확인
    private static final String HLS_OUTPUT_DIR_PATH = "/home/ec2-user/output";

    @Timed("upload.dojob")
    public void doJob(Long spId, Path inputTempFilePath, String fileNameFormat) {
        Path dirPath = null;
        try {
            dirPath = Path.of(HLS_OUTPUT_DIR_PATH + spId);

            directoryManager.mkdirsIfNotExists(dirPath);

            transcodingService.transcode(inputTempFilePath, fileNameFormat, dirPath);

            log.info("S3 Upload started");
            s3FileUploader.uploadSegmentsToS3(dirPath, fileNameFormat, spId);

            redisPublisher.publishTranscodingSuccessEvent(spId);
        } catch (IOException | InterruptedException e) {
            redisPublisher.publishTranscodingFailEvent(spId);

            Thread.currentThread().interrupt();

            throw new RuntimeException("FFmpeg 트랜스코딩 실패", e);
        } finally {
            directoryManager.deleteIfExists(inputTempFilePath);
            directoryManager.deleteIfExists(dirPath);
        }
    }
}

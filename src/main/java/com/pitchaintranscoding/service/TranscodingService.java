package com.pitchaintranscoding.service;

import com.pitchaintranscoding.common.constant.UploadType;
import com.pitchaintranscoding.redis.RedisPublisher;
import com.pitchaintranscoding.upload.FFmpegManager;
import com.pitchaintranscoding.upload.Uploader;
import com.pitchaintranscoding.util.DirectoryManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

@Slf4j
@RequiredArgsConstructor
@Service
public class TranscodingService {
    private final Uploader uploader;
    private final FFmpegManager ffmpegManager;
    private final DirectoryManager directoryManager;
    private final RedisPublisher redisPublisher;

    private static final String HLS_OUTPUT_DIR_PATH = "build/resources/main/sp/";

    public Runnable createJob(Long spId, Path inputTempFilePath, String fileNameFormat) {
        return () -> {
            Path dirPath = null;
            try {
                dirPath = Path.of(HLS_OUTPUT_DIR_PATH + spId);

                prepareOutputDir(dirPath);

                Process process = ffmpegManager.convert(inputTempFilePath, dirPath, fileNameFormat);
                printFFmpegLog(process);

                // 프로세스 완료 대기
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("FFmpeg failed with exit code: " + exitCode);
                }

                log.info("S3 Upload started");
                uploadSegmentsToS3(dirPath, fileNameFormat, spId);

                redisPublisher.publishTranscodingSuccessEvent(spId);
            } catch (IOException | InterruptedException e) {
                redisPublisher.publishTranscodingFailEvent(spId);

                Thread.currentThread().interrupt();

                throw new RuntimeException("FFmpeg 트랜스코딩 실패", e);
            } finally {
                directoryManager.deleteIfExists(inputTempFilePath);
                directoryManager.deleteIfExists(dirPath);
            }
        };
    }

    private void prepareOutputDir(Path dirPath) throws IOException {
        directoryManager.mkdirsIfNotExists(dirPath);
    }

    private void uploadSegmentsToS3(Path dirPath, String fileNameFormat, Long spId) throws IOException {
        for (File localFile : dirPath.toFile().listFiles()) {
            String localFileName = localFile.getName();

            validateFileName(localFileName, fileNameFormat);
            validateFileExtension(localFileName);

            uploader.uploadFileSync(UploadType.SP, spId, localFile);
        }
    }

    private static void validateFileExtension(String fileName) {
        if (!(fileName.endsWith(".m3u8") || fileName.endsWith(".ts")))
            throw new IllegalArgumentException("지원하지 않는 파일 확장자입니다. m3u8 및 ts 파일만 업로드할 수 있습니다.");
    }

    private static void validateFileName(String fileName, String uuidName) {
        if (!fileName.startsWith(uuidName)) {
            log.info("uuidName={}", uuidName);
            log.info("fileName={}", fileName);
            throw new IllegalArgumentException("파일 이름이 UUID와 일치하지 않습니다. 파일 이름은 " + uuidName + "로 시작해야 합니다.");
        }
    }

    private static void printFFmpegLog(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[FFmpeg] {}", line);
            }
        }
    }
}

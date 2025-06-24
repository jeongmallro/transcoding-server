package com.pitchaintranscoding.service;

import com.pitchaintranscoding.upload.FFmpegManager;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

@Slf4j
@RequiredArgsConstructor
@Service
public class TranscodingService {
    private final FFmpegManager ffmpegManager;

    @Timed("upload.transcode")
    public void transcode(Path inputTempFilePath, String fileNameFormat, Path dirPath) throws IOException, InterruptedException {
        Process process = ffmpegManager.convert(inputTempFilePath, dirPath, fileNameFormat);
        printFFmpegLog(process);

        // 프로세스 완료 대기
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("FFmpeg failed with exit code: " + exitCode);
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

package com.pitchaintranscoding.service;

import com.pitchaintranscoding.dto.UploadType;
import com.pitchaintranscoding.upload.FFmpegManager;
import com.pitchaintranscoding.upload.Uploader;
import com.pitchaintranscoding.util.DirectoryManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpUploadService {
    private final Uploader uploader;
    private final FFmpegManager ffmpegManager;
    private final DirectoryManager directoryManager;
    private static final String HLS_OUTPUT_DIR_PATH = "build/resources/main/sp/";

    /**
     * 1. 파일 contentType 검증 - viedo 파일만 허용
     * 2. 트랜스코딩 시작
     *  2.1. 트랜스코딩할 파일 임시 저장
     *  2.2. 변환 파일 저장을 위한 dir 생성
     *   - /build/resources/main/{UploadType.path}/{spId}/{fileNameFormat}.m3u8
     *   - /build/resources/main/{UploadType.path}/{spId}/{fileNameFormat}_%03d.ts
     *  2.3. ffmpeg 트랜스코딩 실행
     * 3. 트랜스 코딩 성공
     *  3.1. 변환된 파일들 S3에 업로드
     *   - /build/resources/main/hls/{spId} 디렉토리 내의 파일들 S3에 업로드
     *  3.2. 메인서버로 업로드 성공 이벤트 발행
     * 4. 트랜스코딩 실패
     *  4.1. 메인서버로 업로드 실패 이벤트 발행
     * 5. 임시 저장 파일 및 저장 dir 삭제
     *  - /build/resources/main/hls/{spId} 디렉토리 삭제
     */
    public void upload(Long spId, MultipartFile file) {
        validateContentType(file);
        
        String fileNameFormat = UUID.randomUUID().toString();

        Path inputTempFilePath = null;
        Path dirPath = null;
        try {
            inputTempFilePath = uploader.createTempFile(file);

            dirPath = Path.of(HLS_OUTPUT_DIR_PATH + spId);
            directoryManager.mkdirsIfNotExists(dirPath);

            Process process = ffmpegManager.convert(inputTempFilePath, dirPath, fileNameFormat);
            printFFmpegLog(process);

            // 프로세스 완료 대기
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("FFmpeg process failed with exit code: " + exitCode);
            }

            uploadSegmentsToS3(dirPath, fileNameFormat, spId);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("FFmpeg 트랜스코딩 실패", e);
        } finally {
            directoryManager.deleteIfExists(inputTempFilePath);
            directoryManager.deleteIfExists(dirPath);
        }
    }

    private void uploadSegmentsToS3(Path dirPath, String fileNameFormat, Long spId) throws IOException {
        for (File localFile : dirPath.toFile().listFiles()) {
            String localFileName = localFile.getName();

            validateFileName(localFileName, fileNameFormat);
            validateFileExtension(localFileName);

            uploader.uploadFileSync(UploadType.SP, spId, localFile);
        }
    }

    private static void validateContentType(MultipartFile file) {
        if (!file.getContentType().startsWith("video")) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. 비디오 파일만 업로드할 수 있습니다.");
        }
    }

    private static void validateFileExtension(String fileName) {
        if (!(fileName.endsWith(".m3u8") || fileName.endsWith(".ts")))
            throw new IllegalArgumentException("지원하지 않는 파일 확장자입니다. m3u8 및 ts 파일만 업로드할 수 있습니다.");
    }

    private static void validateFileName(String fileName, String uuidName) {
        if (!fileName.startsWith(uuidName)) {
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

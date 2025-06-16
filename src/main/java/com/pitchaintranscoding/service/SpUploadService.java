package com.pitchaintranscoding.service;

import com.pitchaintranscoding.util.AsyncTranscodingExecutor;
import com.pitchaintranscoding.upload.Uploader;
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

        // MultipartFile은 HTTP Request 요청 스코프가 끝나면 사라지기 때문에 비동지 작업에서 생성 불가능
        Path inputTempFilePath = uploader.createTempFile(file);

        Runnable job = transcodingService.transcoding(spId, inputTempFilePath, fileNameFormat);
        asyncTranscodingExecutor.execute(spId, job);
    }

    private static void validateContentType(MultipartFile file) {
        if (!file.getContentType().startsWith("video")) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. 비디오 파일만 업로드할 수 있습니다.");
        }
    }
}

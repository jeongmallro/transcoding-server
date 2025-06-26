package com.pitchaintranscoding.upload;

import com.pitchaintranscoding.common.constant.UploadType;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3FileUploader extends FileUploader {
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;
    private static final String BASE_URL = "https://%s.s3.amazonaws.com/%s";
    private static final String S3_OBJECT_KEY_FORMAT = "%s/%d/%s";

    @Timed("upload.s3.upload")
    public void uploadSegmentsToS3(Path dirPath, String fileNameFormat, Long spId) throws IOException {
        for (File localFile : dirPath.toFile().listFiles()) {
            String localFileName = localFile.getName();

            validateFileName(localFileName, fileNameFormat);
            validateFileExtension(localFileName);

            uploadFileSync(UploadType.SP, spId, localFile);
        }
    }

    @Override
    public String uploadFileSync(UploadType type, Long dirIdentifier, File file) throws IOException {
        String s3ObjectKey = generateFileObjectKey(type, dirIdentifier, file.getName());

        PutObjectRequest putObjectRequest = createRequest(s3ObjectKey, file);

        try (InputStream inputStream = new FileInputStream(file)) {
            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(inputStream, file.length())
            );
        } catch (Exception e) {
            log.info("S3 Upload failed because {}", e.getMessage());
            throw new RuntimeException("파일 업로드 중 오류 발생: " + e.getMessage(), e);
        }
        log.info("S3 Upload succeeded {}", s3ObjectKey);
        return s3ObjectKey;
    }

    public String generateFileObjectKey(UploadType type, Long dirIdentifier, String fileName) {
        return S3_OBJECT_KEY_FORMAT.formatted(type.getDirName(), dirIdentifier, fileName);
    }

    private PutObjectRequest createRequest(String s3ObjectKey, File file) throws IOException {
        return PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3ObjectKey)
                .contentType(Files.probeContentType(file.toPath()))
                .contentLength(Files.size(file.toPath()))
                .build();
    }

    private String generateFileUrl(String fileName) {
        return BASE_URL.format(bucketName, fileName);
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


}


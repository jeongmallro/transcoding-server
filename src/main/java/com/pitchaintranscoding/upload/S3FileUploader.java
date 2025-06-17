package com.pitchaintranscoding.upload;

import com.pitchaintranscoding.common.constant.UploadType;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class S3FileUploader extends FileUploader {
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;
    private static final String BASE_URL = "https://%s.s3.amazonaws.com/%s";
    private static final String S3_OBJECT_KEY_FORMAT = "%s/%d/%s";

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
            throw new RuntimeException("파일 업로드 중 오류 발생: " + e.getMessage(), e);
        }

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


}


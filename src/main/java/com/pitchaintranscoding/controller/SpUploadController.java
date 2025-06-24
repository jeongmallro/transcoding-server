package com.pitchaintranscoding.controller;

import com.pitchaintranscoding.service.SpUploadService;
import io.micrometer.core.annotation.Counted;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
public class SpUploadController {
    private final SpUploadService spUploadService;

    @Counted("upload")
    @PostMapping("/sps/{spId}")
    public void upload(@PathVariable Long spId, @RequestParam("file") MultipartFile file) {
        spUploadService.upload(spId, file);
    }

}

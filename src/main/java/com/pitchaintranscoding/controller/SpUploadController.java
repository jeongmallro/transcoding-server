package com.pitchaintranscoding.controller;

import com.pitchaintranscoding.service.SpUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
public class SpUploadController {
    private final SpUploadService spUploadService;

    @PostMapping("/sps/{spId}")
    public void upload(@PathVariable Long spId, @RequestParam("file") MultipartFile file) {
        spUploadService.upload(spId, file);
    }

}

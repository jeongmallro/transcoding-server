package com.pitchaintranscoding.upload;

import com.pitchaintranscoding.dto.FFprobeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RequiredArgsConstructor
@Component
public class FFmpegManager {
    private final FFprobeManager ffprobeManager;

    public Process convert(Path inputFilePath, Path dirPath, String fileName) throws IOException {
        FFprobeResult analyze = ffprobeManager.analyze(inputFilePath);
        List<String> command = FFmpegCommand.of(dirPath, fileName, inputFilePath, analyze).build();

        return new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
    }
}

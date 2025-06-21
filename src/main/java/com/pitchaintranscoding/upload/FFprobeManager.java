package com.pitchaintranscoding.upload;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pitchaintranscoding.dto.FFprobeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

@RequiredArgsConstructor
@Configuration
public class FFprobeManager {
    private final ObjectMapper mapper;

    private static final String ffprobePath = "C:\\Users\\jeong\\Desktop\\project\\transcoding-server\\src\\main\\resources\\bin\\ffprobe.exe"; //

    public FFprobeResult analyze(Path originalVideoPath) throws IOException {
        try {
            Process process = convert(originalVideoPath);
            String json = readJson(process);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("FFprobe failed with exit code: " + exitCode);
            }

            return parseFFprobeResult(json);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("FFprobe process interrupted", e);
        }
    }

    private String readJson(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            return output.toString();
        }
    }

    private Process convert(Path originalVideoPath) throws IOException {
        List<String> command = List.of(
                ffprobePath,
                "-v", "error",
                "-show_entries", "stream=codec_type,width,height,r_frame_rate",
                "-of", "json",
                originalVideoPath.toString()
        );

        return new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
    }

    private FFprobeResult parseFFprobeResult(String json) throws IOException {
        JsonNode root = mapper.readTree(json);
        JsonNode streams = root.path("streams");

        JsonNode videoStream = null;
        boolean hasAudio = false;

        for (JsonNode stream : streams) {
            String codecType = stream.path("codec_type").asText();
            switch (codecType) {
                case "video" -> videoStream = stream;
                case "audio" -> hasAudio = true;
            }
        }

        if (videoStream == null) {
            throw new IOException("No video stream found");
        }

        int width = videoStream.path("width").asInt();
        int height = videoStream.path("height").asInt();
        double fps = calculateFps(videoStream.path("r_frame_rate").asText());

        return FFprobeResult.of(width, height, fps, hasAudio);
    }

    private double calculateFps(String frameRate) {
        String[] parts = frameRate.split("/");
        if (parts.length == 2) {
            double numerator = Double.parseDouble(parts[0]);
            double denominator = Double.parseDouble(parts[1]);
            return numerator / denominator;
        }
        return Double.parseDouble(frameRate);
    }
}

package com.pitchaintranscoding.upload;

import com.pitchaintranscoding.dto.AudioStream;
import com.pitchaintranscoding.dto.FFprobeResult;
import com.pitchaintranscoding.dto.VideoStream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FFmpegCommand {
    private static final String FFMPEG_PATH = "src/main/resources/bin/ffmpeg";
    private static final String M3U8_FORMAT = "%s/%s_%%v.m3u8";
    private static final String TS_FORMAT = "%s/%s_%%v_%%03d.ts";

    private final Path dirPath;
    private final String fileName;
    private final Path inputFilePath;
    private final VideoStream videoStream;
    private final AudioStream audioStream;

    public static FFmpegCommand of(Path dirPath, String fileName, Path inputFilePath, FFprobeResult probeResult) {
        return new FFmpegCommand(dirPath, fileName, inputFilePath, probeResult.toVideoStream(), probeResult.toAudioStream());
    }

    public List<String> build() {
        List<String> command = new ArrayList<>();
        command.addAll(List.of(FFMPEG_PATH, "-i", inputFilePath.toString()));
        command.addAll(List.of("-filter_complex", buildFilterComplex()));
        command.addAll(buildMaps());
        command.addAll(List.of("-c:v", "libx264"));
        if (audioStream.exists()) {
            command.addAll(List.of("-c:a", "aac"));
        }
        command.addAll(buildBitrates());
        command.addAll(List.of(
                "-var_stream_map", buildVarStreamMap(),
                "-f", "hls",
                "-hls_time", "10",
                "-hls_list_size", "0",
                "-hls_segment_type", "mpegts",
                "-hls_segment_filename", TS_FORMAT.formatted(dirPath, fileName),
                M3U8_FORMAT.formatted(dirPath, fileName)
        ));

        return command;
    }

    private String buildFilterComplex() {
        List<String> scales = videoStream.getScales();

        String splitCount = String.format("[0:v]split=%d", scales.size());
        String outputs = scales.stream()
                .map(s -> String.format("[v%d]", scales.indexOf(s) + 1))
                .collect(Collectors.joining(""));

        return splitCount + outputs + ";" + String.join(";", scales);
    }

    private List<String> buildBitrates() {
        List<String> bitrates = new ArrayList<>();
        List<String> qualities = videoStream.getAvailableQualities();

        for (int i = 0; i < qualities.size(); i++) {
            bitrates.add("-b:v:" + i);
            bitrates.add(VideoStream.BITRATE.get(qualities.get(i)));
        }

        return bitrates;
    }

    private String buildVarStreamMap() {
        List<String> streamMaps = new ArrayList<>();
        List<String> qualities = videoStream.getAvailableQualities();

        for (int i = 0; i < qualities.size(); i++) {
            String quality = qualities.get(i);
            streamMaps.add(audioStream.exists()
                    ? String.format("v:%d,a:%d,name:%sp", i, i, quality)
                    : String.format("v:%d,name:%sp", i, quality));
        }

        return String.join(" ", streamMaps);
    }

    private List<String> buildMaps() {
        List<String> maps = new ArrayList<>();
        List<String> qualities = videoStream.getAvailableQualities();

        for (String quality : qualities) {
            maps.add("-map");
            maps.add(String.format("[v%sp]", quality));
            if (audioStream.exists()) {
                maps.add("-map");
                maps.add("0:a");
            }
        }
        return maps;
    }

}
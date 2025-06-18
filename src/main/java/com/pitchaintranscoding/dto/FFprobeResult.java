package com.pitchaintranscoding.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FFprobeResult {
    private int width;
    private int height;
    private double fps;
    private boolean hasAudio;

    public static FFprobeResult of(int width, int height, double fps, boolean hasAudio) {
        FFprobeResult result = new FFprobeResult();
        result.width = width;
        result.height = height;
        result.fps = fps;
        result.hasAudio = hasAudio;
        return result;
    }

    public AudioStream toAudioStream() {
        return AudioStream.of(this.hasAudio);
    }

    public VideoStream toVideoStream() {
        return new VideoStream(this.width, this.height, this.fps);
    }
}

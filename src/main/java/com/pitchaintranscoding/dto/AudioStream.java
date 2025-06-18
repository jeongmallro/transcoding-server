package com.pitchaintranscoding.dto;

public record AudioStream(boolean exists) {

    public static AudioStream of(boolean hasAudio) {
        return new AudioStream(hasAudio);
    }
}
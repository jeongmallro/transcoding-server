package com.pitchaintranscoding.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum UploadType {
    SP("sp"),
    ;

    private final String dirName;
}

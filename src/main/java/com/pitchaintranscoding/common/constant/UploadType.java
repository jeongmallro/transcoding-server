package com.pitchaintranscoding.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum UploadType {
    SP("sp"),
    ;

    private final String dirName;
}

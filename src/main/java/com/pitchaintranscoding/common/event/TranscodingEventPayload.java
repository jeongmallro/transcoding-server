package com.pitchaintranscoding.common.event;

import lombok.Getter;

@Getter
public class TranscodingEventPayload implements EventPayload {
    private Long spId;

    public static TranscodingEventPayload of(Long spId) {
        TranscodingEventPayload payload = new TranscodingEventPayload();
        payload.spId = spId;
        return payload;
    }
}

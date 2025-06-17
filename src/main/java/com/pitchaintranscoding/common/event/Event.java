package com.pitchaintranscoding.common.event;

import com.pitchaintranscoding.common.constant.EventType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Event<T extends EventPayload> {
    private EventType eventType;
    private T payload;

    public static <T extends EventPayload> Event<T> of(EventType eventType, T payload) {
        Event<T> event = new Event<>();
        event.eventType = eventType;
        event.payload = payload;
        return event;
    }
}

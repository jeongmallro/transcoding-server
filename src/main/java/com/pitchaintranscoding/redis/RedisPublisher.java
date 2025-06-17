package com.pitchaintranscoding.redis;

import com.pitchaintranscoding.common.constant.EventType;
import com.pitchaintranscoding.common.event.Event;
import com.pitchaintranscoding.common.event.EventPayload;
import com.pitchaintranscoding.common.event.TranscodingEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RedisPublisher {
    private final RedisTemplate redisTemplate;

    @Value("${spring.redis.channel}")
    private String channel;

    public void publishTranscodingSuccessEvent(Long spId) {
        Event<TranscodingEventPayload> event = Event.of(EventType.TRANSCODING_COMPLETED, TranscodingEventPayload.of(spId));
        sendEvent(event);
    }
    public void publishTranscodingFailEvent(Long spId) {
        Event<TranscodingEventPayload> event = Event.of(EventType.TRANSCODING_FAILED, TranscodingEventPayload.of(spId));
        sendEvent(event);
    }


    private Long sendEvent(Event<? extends EventPayload> event) {
        return redisTemplate.convertAndSend(channel, event);
    }
}
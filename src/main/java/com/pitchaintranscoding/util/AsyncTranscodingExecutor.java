package com.pitchaintranscoding.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncTranscodingExecutor {
    private final List<Long> activeTranscoding = new CopyOnWriteArrayList<>();
    private final ExecutorService executor;

    public void execute(Long key, Runnable runnable) throws RejectedExecutionException {
        activeTranscoding.add(key);

        executor.submit(() -> {
            try {
                runnable.run();
            } finally {
                activeTranscoding.remove(key);
            }
        });
    }

    @EventListener(ContextClosedEvent.class)
    public void shutdown() {
        activeTranscoding.forEach(key -> {
            // todo 메인 서버로 업로드 실패 이벤트 발행
        });
    }
}

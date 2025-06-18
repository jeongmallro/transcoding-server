package com.pitchaintranscoding.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
public class ExecutorConfig {

    @Bean
    public BlockingQueue<Runnable> videoQueue() {
        return new LinkedBlockingQueue<>();
    }

    @Bean
    public ThreadPoolExecutor videoExecutor() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            private final String namePrefix = "video-exec-";

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = defaultFactory.newThread(r);
                thread.setName(namePrefix + threadNumber.getAndIncrement());
                thread.setUncaughtExceptionHandler((t, e) ->
                        log.error("Thread {} threw exception: {}", t.getName(), e.getMessage(), e));
                return thread;
            }
        };

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                5, // 코어 스레드 수
                5, // 최대 스레드 수
                0L,
                TimeUnit.MILLISECONDS,
                videoQueue(),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy() // 거부된 작업 처리 정책
        );

        return executor;
    }

}
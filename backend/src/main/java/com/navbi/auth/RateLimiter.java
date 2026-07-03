package com.navbi.auth;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** 单机内存固定窗口限流：单实例部署下无需引入外部依赖。 */
@Component
public class RateLimiter {

    private record Window(long startMillis, long windowMillis, AtomicInteger count) {

        boolean expired(long now) {
            return now - startMillis >= windowMillis;
        }
    }

    private static final int PURGE_THRESHOLD = 50_000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /** 窗口内第 limit+1 次起返回 false。 */
    public boolean tryAcquire(String key, int limit, Duration window) {
        long now = System.currentTimeMillis();
        purgeIfOversized(now);
        Window current = windows.compute(key, (k, old) ->
                old == null || old.expired(now)
                        ? new Window(now, window.toMillis(), new AtomicInteger())
                        : old);
        return current.count().incrementAndGet() <= limit;
    }

    public void clear() {
        windows.clear();
    }

    private void purgeIfOversized(long now) {
        if (windows.size() > PURGE_THRESHOLD) {
            windows.entrySet().removeIf(e -> e.getValue().expired(now));
        }
    }
}

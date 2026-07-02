package com.navbi.counter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Redis 实时计数。Redis 不可用时全部方法静默降级（返回 null / false），
 * 由调用方回退到数据库口径，保证埋点与点击链路永不因 Redis 故障失败。
 */
@Slf4j
@Service
public class CounterService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Duration DAILY_TTL = Duration.ofHours(48);

    private final StringRedisTemplate redis;
    private volatile boolean warned;

    public CounterService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void recordVisit(String sessionId, String ip) {
        String day = LocalDate.now().format(DAY);
        try {
            String pvKey = "navbi:pv:" + day;
            redis.opsForValue().increment(pvKey);
            redis.expire(pvKey, DAILY_TTL);
            String uvKey = "navbi:uv:" + day;
            redis.opsForHyperLogLog().add(uvKey, sessionId);
            redis.expire(uvKey, DAILY_TTL);
            String ipKey = "navbi:ip:" + day;
            redis.opsForHyperLogLog().add(ipKey, ip);
            redis.expire(ipKey, DAILY_TTL);
        } catch (Exception e) {
            warnOnce(e);
        }
    }

    /** 今日 PV，Redis 不可用时返回 null。 */
    public Long getTodayPv() {
        try {
            String value = redis.opsForValue().get("navbi:pv:" + LocalDate.now().format(DAY));
            return value == null ? 0L : Long.parseLong(value);
        } catch (Exception e) {
            warnOnce(e);
            return null;
        }
    }

    /** 今日 UV，Redis 不可用时返回 null。 */
    public Long getTodayUv() {
        try {
            return redis.opsForHyperLogLog().size("navbi:uv:" + LocalDate.now().format(DAY));
        } catch (Exception e) {
            warnOnce(e);
            return null;
        }
    }

    /** 该 sessionId 是否首次点击此导航；Redis 不可用时返回 false（UV 不计）。 */
    public boolean isNewNavClicker(Long navId, String sessionId) {
        try {
            Long added = redis.opsForSet().add("navbi:navclick:" + navId, sessionId);
            return added != null && added == 1;
        } catch (Exception e) {
            warnOnce(e);
            return false;
        }
    }

    private void warnOnce(Exception e) {
        if (!warned) {
            warned = true;
            log.warn("Redis 不可用，实时计数降级为数据库口径: {}", e.getMessage());
        }
    }
}

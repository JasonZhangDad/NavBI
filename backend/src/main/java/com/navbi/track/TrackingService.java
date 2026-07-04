package com.navbi.track;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class TrackingService {

    private final VisitLogMapper visitLogMapper;
    private final UserSessionMapper userSessionMapper;
    private final UaParser uaParser;
    private final GeoResolver geoResolver;

    public TrackingService(VisitLogMapper visitLogMapper, UserSessionMapper userSessionMapper,
                           UaParser uaParser, GeoResolver geoResolver) {
        this.visitLogMapper = visitLogMapper;
        this.userSessionMapper = userSessionMapper;
        this.uaParser = uaParser;
        this.geoResolver = geoResolver;
    }

    @Async("trackExecutor")
    public void recordAsync(String ip, String userAgent, String countryCode,
                            String url, String referer, String sessionId) {
        try {
            record(ip, userAgent, countryCode, url, referer, sessionId);
        } catch (Exception e) {
            log.error("访问日志写入失败 ip={} url={}", ip, url, e);
        }
    }

    void record(String ip, String userAgent, String countryCode, String url, String referer, String sessionId) {
        UaInfo ua = uaParser.parse(userAgent);
        GeoInfo geo = geoResolver.resolve(ip, countryCode);

        VisitLog visitLog = new VisitLog();
        visitLog.setIp(ip);
        visitLog.setCountry(geo.country());
        visitLog.setProvince(geo.province());
        visitLog.setCity(geo.city());
        visitLog.setDevice(ua.device());
        visitLog.setOs(ua.os());
        visitLog.setBrowser(ua.browser());
        visitLog.setUrl(truncate(url, 512));
        visitLog.setReferer(truncate(referer, 512));
        visitLog.setUserAgent(truncate(userAgent, 1024));
        visitLog.setSessionId(sessionId);
        visitLog.setCreatedAt(LocalDateTime.now());
        visitLogMapper.insert(visitLog);

        if (userSessionMapper.touch(sessionId) == 0) {
            UserSession session = new UserSession();
            session.setSessionId(sessionId);
            session.setIp(ip);
            session.setStartTime(LocalDateTime.now());
            session.setEndTime(LocalDateTime.now());
            session.setPageCount(1);
            try {
                userSessionMapper.insert(session);
            } catch (Exception e) {
                // 并发下 session_id 唯一约束冲突：退回 touch
                userSessionMapper.touch(sessionId);
            }
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}

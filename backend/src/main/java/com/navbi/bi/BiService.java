package com.navbi.bi;

import com.navbi.counter.CounterService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BiService {

    private final StatMapper statMapper;
    private final CounterService counterService;

    public BiService(StatMapper statMapper, CounterService counterService) {
        this.statMapper = statMapper;
        this.counterService = counterService;
    }

    public SummaryVo summary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime now = LocalDateTime.now();

        // "今日"优先取 Redis 实时值，Redis 不可用时回退数据库
        Long todayPv = counterService.getTodayPv();
        if (todayPv == null || todayPv == 0) {
            todayPv = statMapper.countPvBetween(todayStart, now);
        }
        Long todayUv = counterService.getTodayUv();
        if (todayUv == null || todayUv == 0) {
            todayUv = statMapper.countUvBetween(todayStart, now);
        }
        return new SummaryVo(todayPv, todayUv,
                statMapper.countPvBetween(yesterdayStart, todayStart),
                statMapper.totalPv(), statMapper.totalUv(),
                statMapper.countIpBetween(todayStart, now), statMapper.distinctIpCount(),
                statMapper.countUsersBetween(todayStart, now));
    }

    public List<TrendPoint> trend(String dimension) {
        return switch (dimension) {
            case "hour" -> statMapper.trendByHour(LocalDateTime.now().minusHours(24));
            case "day" -> statMapper.trendByDay(LocalDate.now().minusDays(29).atStartOfDay());
            default -> throw new IllegalArgumentException("dimension 仅支持 hour 或 day");
        };
    }

    public List<NameValue> topPages() {
        return statMapper.topPages();
    }

    public List<RegisterTrendPoint> registerTrend() {
        return statMapper.registerTrendByDay(LocalDate.now().minusDays(29).atStartOfDay());
    }

    public List<NameValue> registerGeo() {
        return statMapper.registerGeo();
    }

    public List<NameValue> deviceShare() {
        return statMapper.deviceShare();
    }

    public List<NameValue> browserShare() {
        return statMapper.browserShare();
    }

    public List<NameValue> geo(String level) {
        return switch (level) {
            case "country" -> statMapper.geoByCountry();
            case "province" -> statMapper.geoByProvince();
            default -> throw new IllegalArgumentException("level 仅支持 country 或 province");
        };
    }

    public LogPage logs(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        long offset = (long) Math.max(page - 1, 0) * safeSize;
        return new LogPage(statMapper.totalPv(), statMapper.pageLogs(safeSize, offset));
    }
}

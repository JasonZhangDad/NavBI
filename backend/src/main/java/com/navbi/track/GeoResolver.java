package com.navbi.track;

import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 基于 ip2region 离线 xdb 的地域解析。xdb 全量加载进内存，单次查询微秒级。
 * xdb 文件缺失或 IP 无法解析（如 IPv6）时返回"未知"，不阻断埋点链路。
 */
@Slf4j
@Component
public class GeoResolver {

    private final Searcher searcher;

    public GeoResolver() {
        Searcher loaded = null;
        try {
            ClassPathResource resource = new ClassPathResource("ip2region/ip2region.xdb");
            loaded = Searcher.newWithBuffer(resource.getContentAsByteArray());
        } catch (Exception e) {
            log.warn("ip2region.xdb 加载失败，地域解析降级为未知: {}", e.getMessage());
        }
        this.searcher = loaded;
    }

    public GeoInfo resolve(String ip) {
        if (ip == null || ip.isBlank()) {
            return GeoInfo.UNKNOWN;
        }
        if (isPrivate(ip)) {
            return GeoInfo.INTRANET;
        }
        if (searcher == null) {
            return GeoInfo.UNKNOWN;
        }
        try {
            // 格式: 国家|区域|省份|城市|ISP，占位符为 "0"
            String[] parts = searcher.search(ip).split("\\|");
            return new GeoInfo(part(parts, 0), part(parts, 2), part(parts, 3));
        } catch (Exception e) {
            return GeoInfo.UNKNOWN;
        }
    }

    private static String part(String[] parts, int index) {
        return parts.length > index && !parts[index].isBlank() && !"0".equals(parts[index])
                ? parts[index] : "未知";
    }

    private static boolean isPrivate(String ip) {
        if (ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("127.")
                || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return true;
        }
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length > 1) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return false;
    }
}

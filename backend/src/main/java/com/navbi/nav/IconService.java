package com.navbi.nav;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端代理抓取导航站点 logo：浏览器只请求本站，避免第三方图标源
 * 在部分网络环境（如大陆屏蔽 DDG）不可达的问题。结果内存缓存，导航变更时失效。
 */
@Slf4j
@Service
public class IconService {

    public record Icon(byte[] bytes, String contentType) {
    }

    private static final Set<String> TWO_PART_TLD =
            Set.of("com.cn", "net.cn", "org.cn", "gov.cn", "edu.cn", "co.uk", "com.hk", "com.tw");

    private final Map<Long, Optional<Icon>> cache = new ConcurrentHashMap<>();
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public Optional<Icon> iconFor(NavItem item) {
        return cache.computeIfAbsent(item.getId(), id -> fetchChain(item));
    }

    public void evict(Long id) {
        cache.remove(id);
    }

    private Optional<Icon> fetchChain(NavItem item) {
        for (String url : candidates(item)) {
            Optional<Icon> icon = fetch(url);
            if (icon.isPresent()) {
                return icon;
            }
        }
        log.info("导航 {} 所有图标源均失败", item.getId());
        return Optional.empty();
    }

    List<String> candidates(NavItem item) {
        List<String> list = new ArrayList<>();
        if (item.getIcon() != null && item.getIcon().startsWith("http")) {
            list.add(item.getIcon());
        }
        try {
            String host = URI.create(item.getUrl()).getHost();
            if (host != null) {
                list.add("https://icons.duckduckgo.com/ip3/" + host + ".ico");
                String root = rootDomain(host);
                if (!root.equals(host)) {
                    list.add("https://icons.duckduckgo.com/ip3/" + root + ".ico");
                }
                list.add("https://" + host + "/favicon.ico");
            }
        } catch (IllegalArgumentException ignored) {
            // URL 非法：仅尝试手动图标
        }
        return list;
    }

    private Optional<Icon> fetch(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "Mozilla/5.0 NavBI-IconFetcher")
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            byte[] body = response.body();
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (response.statusCode() == 200 && body.length > 0 && looksLikeImage(contentType, body)) {
                return Optional.of(new Icon(body, contentType.isBlank() ? "image/x-icon" : contentType));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.debug("图标抓取失败 {}: {}", url, e.getMessage());
        }
        return Optional.empty();
    }

    static boolean looksLikeImage(String contentType, byte[] body) {
        if (contentType != null && contentType.startsWith("image/")) {
            return true;
        }
        if (body.length < 4) {
            return false;
        }
        // PNG / GIF / JPEG / ICO / RIFF(webp) / SVG 魔数
        if ((body[0] & 0xFF) == 0x89 && body[1] == 'P') return true;
        if (body[0] == 'G' && body[1] == 'I' && body[2] == 'F') return true;
        if ((body[0] & 0xFF) == 0xFF && (body[1] & 0xFF) == 0xD8) return true;
        if (body[0] == 0 && body[1] == 0 && body[2] == 1 && body[3] == 0) return true;
        if (body[0] == 'R' && body[1] == 'I' && body[2] == 'F' && body[3] == 'F') return true;
        String head = new String(body, 0, Math.min(body.length, 256), StandardCharsets.UTF_8);
        return head.contains("<svg");
    }

    static String rootDomain(String host) {
        String[] parts = host.split("\\.");
        if (parts.length <= 2) {
            return host;
        }
        String lastTwo = parts[parts.length - 2] + "." + parts[parts.length - 1];
        return TWO_PART_TLD.contains(lastTwo)
                ? parts[parts.length - 3] + "." + lastTwo
                : lastTwo;
    }
}

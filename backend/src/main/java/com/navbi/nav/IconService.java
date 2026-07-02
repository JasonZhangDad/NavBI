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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 服务端代理抓取导航站点 logo：浏览器只请求本站，规避第三方图标源
 * 在部分网络环境不可达的问题。手动配置的图标 URL 可用时优先；否则抓取
 * 全部自动源，用魔数校验真伪（不信任 Content-Type）。精确 host 源优先，
 * 根域名只作兜底；同一优先级内取字节数最大者。成功结果内存缓存，导航变更时失效。
 */
@Slf4j
@Service
public class IconService {

    public record Icon(byte[] bytes, String contentType) {
    }

    private static final Set<String> TWO_PART_TLD =
            Set.of("com.cn", "net.cn", "org.cn", "gov.cn", "edu.cn", "co.uk", "com.hk", "com.tw");

    private record IconCandidate(String url, int priority) {
    }

    private final Map<Long, Icon> cache = new ConcurrentHashMap<>();
    private final Function<String, Optional<Icon>> fetcher;
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public IconService() {
        this.fetcher = this::fetch;
    }

    IconService(Function<String, Optional<Icon>> fetcher) {
        this.fetcher = fetcher;
    }

    public Optional<Icon> iconFor(NavItem item) {
        Icon cached = cache.get(item.getId());
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<Icon> fetched = fetchBest(item);
        fetched.ifPresent(icon -> cache.put(item.getId(), icon));
        return fetched;
    }

    public void evict(Long id) {
        cache.remove(id);
    }

    private Optional<Icon> fetchBest(NavItem item) {
        if (item.getIcon() != null && item.getIcon().startsWith("http")) {
            Optional<Icon> manual = fetcher.apply(item.getIcon());
            if (manual.isPresent()) {
                return manual;
            }
        }
        List<Icon> valid = new ArrayList<>();
        Integer priority = null;
        for (IconCandidate candidate : autoCandidateSpecs(item)) {
            if (priority != null && priority != candidate.priority() && !valid.isEmpty()) {
                return pickBest(valid);
            }
            if (priority == null || priority != candidate.priority()) {
                valid.clear();
                priority = candidate.priority();
            }
            fetcher.apply(candidate.url()).ifPresent(valid::add);
        }
        Optional<Icon> best = pickBest(valid);
        if (best.isEmpty()) {
            log.info("导航 {} 所有图标源均失败", item.getId());
        }
        return best;
    }

    static Optional<Icon> pickBest(List<Icon> icons) {
        return icons.stream().max(Comparator.comparingInt(icon -> icon.bytes().length));
    }

    List<String> autoCandidates(NavItem item) {
        return autoCandidateSpecs(item).stream()
                .map(IconCandidate::url)
                .toList();
    }

    private List<IconCandidate> autoCandidateSpecs(NavItem item) {
        List<IconCandidate> list = new ArrayList<>();
        try {
            String host = URI.create(item.getUrl()).getHost();
            if (host != null) {
                list.add(new IconCandidate("https://icons.duckduckgo.com/ip3/" + host + ".ico", 0));
                list.add(new IconCandidate("https://www.google.com/s2/favicons?domain=" + host + "&sz=64", 0));
                list.add(new IconCandidate("https://" + host + "/favicon.ico", 0));
                String root = rootDomain(host);
                if (!root.equals(host)) {
                    list.add(new IconCandidate("https://icons.duckduckgo.com/ip3/" + root + ".ico", 1));
                    list.add(new IconCandidate("https://www.google.com/s2/favicons?domain=" + root + "&sz=64", 1));
                }
            }
        } catch (IllegalArgumentException ignored) {
            // URL 非法：无自动源
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
            if (response.statusCode() == 200 && looksLikeImage(body)) {
                String contentType = response.headers().firstValue("Content-Type").orElse("");
                return Optional.of(new Icon(body, contentType.startsWith("image/") ? contentType : "image/x-icon"));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.debug("图标抓取失败 {}: {}", url, e.getMessage());
        }
        return Optional.empty();
    }

    /** 仅认魔数：部分源会用 image/* 的 Content-Type 返回非图片内容。 */
    static boolean looksLikeImage(byte[] body) {
        if (body.length < 4) {
            return false;
        }
        // PNG / GIF / JPEG / ICO / RIFF(webp) / SVG
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
